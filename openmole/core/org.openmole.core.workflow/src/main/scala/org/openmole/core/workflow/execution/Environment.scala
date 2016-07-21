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

import java.util.logging.Level

import org.openmole.core.event.{ Event, EventAccumulator, EventDispatcher }
import org.openmole.core.workflow.execution.local.{ ExecutorPool, LocalExecutionJob }
import org.openmole.core.workflow.job.Job
import org.openmole.core.workflow.job.MoleJob
import ExecutionState._
import org.openmole.core.workflow.mole.MoleExecution
import org.openmole.core.workflow.task.TaskExecutionContext
import org.openmole.core.workflow.tools.{ ExceptionEvent, Name }
import org.openmole.core.workspace.{ ConfigurationLocation, Workspace }
import org.openmole.tool.collection._

import scala.concurrent.stm._
import org.openmole.core.tools.service._
import org.openmole.core.workflow.dsl._
import org.openmole.tool.cache._

import scala.ref.WeakReference

object Environment {
  val maxExceptionsLog = ConfigurationLocation("Environment", "MaxExceptionsLog", Some(1000))

  Workspace setDefault maxExceptionsLog

  case class JobSubmitted(job: ExecutionJob) extends Event[Environment]
  case class JobStateChanged(job: ExecutionJob, newState: ExecutionState, oldState: ExecutionState) extends Event[Environment]
  case class ExceptionRaised(job: ExecutionJob, exception: Throwable, level: Level) extends Event[Environment] with ExceptionEvent
  case class MoleJobExceptionRaised(job: ExecutionJob, exception: Throwable, level: Level, moleJob: MoleJob) extends Event[Environment] with ExceptionEvent
  case class JobCompleted(job: ExecutionJob, log: RuntimeLog, info: RuntimeInfo) extends Event[Environment]

  case class RuntimeLog(beginTime: Long, executionBeginTime: Long, executionEndTime: Long, endTime: Long)
}

import Environment._

sealed trait Environment <: Name {
  private[execution] var _done = 0L
  private[execution] var _failed = 0L

  private lazy val _errors = new SlidingList[ExceptionEvent](() ⇒ Workspace.preference(maxExceptionsLog))
  def error(e: ExceptionEvent) = _errors.put(e)
  def errors: List[ExceptionEvent] = _errors.elements
  def clearErrors: List[ExceptionEvent] = _errors.clear()

  def submitted: Long
  def running: Long
  def done: Long = _done
  def failed: Long = _failed

}

trait SubmissionEnvironment <: Environment {
  def submit(job: Job)
  def jobs: Iterable[ExecutionJob]
}

object LocalEnvironment {

  val DefaultNumberOfThreads = ConfigurationLocation("LocalExecutionEnvironment", "ThreadNumber", Some(1))

  Workspace setDefault DefaultNumberOfThreads
  var defaultNumberOfThreads = Workspace.preference(DefaultNumberOfThreads)

  def apply(
    nbThreads:    Int                      = defaultNumberOfThreads,
    deinterleave: Boolean                  = false,
    name:         OptionalArgument[String] = OptionalArgument()
  ) = new LocalEnvironment(nbThreads, deinterleave, name)

}

class LocalEnvironment(
    val nbThreads:     Int,
    val deinterleave:  Boolean,
    override val name: Option[String]
) extends Environment {

  val pool = Cache(new ExecutorPool(nbThreads, WeakReference(this)))

  def nbJobInQueue = pool().waiting

  def submit(job: Job, executionContext: TaskExecutionContext): Unit =
    submit(new LocalExecutionJob(executionContext, job.moleJobs, Some(job.moleExecution)))

  def submit(moleJob: MoleJob, executionContext: TaskExecutionContext): Unit =
    submit(new LocalExecutionJob(executionContext, List(moleJob), None))

  private def submit(ejob: LocalExecutionJob): Unit = {
    pool().enqueue(ejob)
    ejob.state = ExecutionState.SUBMITTED
    EventDispatcher.trigger(this, new Environment.JobSubmitted(ejob))
  }

  def submitted: Long = pool().waiting
  def running: Long = pool().running
}
