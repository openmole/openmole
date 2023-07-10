/*
 * Copyright (C) 2010 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.workflow.execution

import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Level
import org.openmole.core.event.{ Event, EventDispatcher }
import org.openmole.core.preference.{ Preference, PreferenceLocation }
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.tools.service._
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.execution.ExecutionState._
import org.openmole.core.workflow.job.{ JobGroup, Job, JobId }
import org.openmole.core.workflow.mole.MoleExecution
import org.openmole.core.workflow.task.TaskExecutionContext
import org.openmole.core.workflow.tools.Name
import org.openmole.tool.cache._

import scala.ref.WeakReference

object Environment {
  val maxExceptionsLog = PreferenceLocation("Environment", "MaxExceptionsLog", Some(200))

  case class JobSubmitted(id: Long, job: ExecutionJob) extends Event[Environment]
  case class JobStateChanged(id: Long, job: ExecutionJob, newState: ExecutionState, oldState: ExecutionState) extends Event[Environment]
  case class ExceptionRaised(exception: Throwable, level: Level, detail: Option[String] = None) extends Event[Environment] with ExceptionEvent
  case class ExecutionJobExceptionRaised(job: ExecutionJob, exception: Throwable, level: Level, detail: Option[String] = None) extends Event[Environment] with ExceptionEvent
  case class MoleJobExceptionRaised(job: ExecutionJob, exception: Throwable, level: Level, moleJob: JobId, detail: Option[String] = None) extends Event[Environment] with ExceptionEvent

  case class JobCompleted(job: ExecutionJob, log: RuntimeLog, info: RuntimeInfo) extends Event[Environment]

  case class RuntimeLog(beginTime: Long, executionBeginTime: Long, executionEndTime: Long, endTime: Long)

  def errors(environment: Environment) =
    environment match {
      case e: SubmissionEnvironment ⇒ e.errors
      case _: LocalEnvironment      ⇒ Seq()
    }

  def clearErrors(environment: Environment) =
    environment match {
      case e: SubmissionEnvironment ⇒ e.clearErrors
      case _                        ⇒ Seq()
    }

  def submit(environment: Environment, job: JobGroup): Long = {
    val moleExecution = JobGroup.moleExecution(job)

    environment match {
      case env: SubmissionEnvironment ⇒ env.submit(job)
      case env: LocalEnvironment ⇒
        env.submit(
          job,
          moleExecution.partialTaskExecutionContext
        )
    }
  }

}

sealed trait Environment extends Name {
  def submitted: Long
  def running: Long
  def done: Long
  def failed: Long

  def start(): Unit
  def stop(): Unit
}

/**
 * An environment with the properties of submitting jobs, getting jobs, and cleaning.
 *
 * This trait is implemented by environment plugins, and not the more generic [[Environment]]
 */
trait SubmissionEnvironment extends Environment {
  def submit(job: JobGroup): Long
  def jobs: Iterable[ExecutionJob]
  def runningJobs: Seq[ExecutionJob]

  def clean: Boolean
  def errors: Seq[ExceptionEvent]
  def clearErrors: Seq[ExceptionEvent]
}

object LocalEnvironment {

  def apply(
    threads:      OptionalArgument[Int]    = None,
    deinterleave: Boolean                  = false,
    name:         OptionalArgument[String] = None
  )(implicit varName: sourcecode.Name) =
    EnvironmentProvider { ms ⇒
      import ms._
      new LocalEnvironment(threads.getOrElse(1), deinterleave, Some(name.getOrElse(varName.value)))
    }

  def apply(threads: Int, deinterleave: Boolean) =
    EnvironmentProvider { ms ⇒
      import ms._
      new LocalEnvironment(threads, deinterleave, None)
    }

}

/**
 * Local environment
 * @param threads number of parallel threads
 * @param deinterleave get the outputs of executions as strings
 */
class LocalEnvironment(
  val threads:       Int,
  val deinterleave:  Boolean,
  override val name: Option[String]
)(implicit val threadProvider: ThreadProvider, val eventDispatcherService: EventDispatcher) extends Environment {

  val pool = Cache(new ExecutorPool(threads, WeakReference(this), threadProvider))

  def runningJobs = pool().runningJobs
  def nbJobInQueue = pool().waiting

  def submit(job: JobGroup, executionContext: TaskExecutionContext.Partial): Long = {
    val id = jobId.getAndIncrement()
    submit(LocalExecutionJob(id, executionContext, JobGroup.moleJobs(job), Some(JobGroup.moleExecution(job))))
    id
  }

  def submit(moleJob: Job, executionContext: TaskExecutionContext.Partial): Long = {
    val id = jobId.getAndIncrement()
    submit(LocalExecutionJob(id, executionContext, List(moleJob), None))
    id
  }

  private def submit(ejob: LocalExecutionJob): Unit = {
    pool().enqueue(ejob)
    eventDispatcherService.trigger(this, Environment.JobSubmitted(ejob.id, ejob))
    eventDispatcherService.trigger(this, Environment.JobStateChanged(ejob.id, ejob, SUBMITTED, READY))
  }

  def submitted: Long = pool().waiting
  def running: Long = pool().running

  override def start() = {}
  override def stop() = pool().stop()

  private val jobId = new AtomicLong(0L)
  private[execution] val _done = new AtomicLong(0L)
  private[execution] val _failed = new AtomicLong(0L)

  def done: Long = _done.get()
  def failed: Long = _failed.get()

}
