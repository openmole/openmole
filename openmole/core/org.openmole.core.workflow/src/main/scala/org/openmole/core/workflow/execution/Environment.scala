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
import org.openmole.core.event.{ EventDispatcher, EventAccumulator, Event }
import org.openmole.core.workflow.execution.local.{ LocalExecutionJob, ExecutorPool }
import org.openmole.core.workflow.job.Job
import org.openmole.core.workflow.job.MoleJob
import ExecutionState._
import org.openmole.core.workflow.mole.MoleExecution
import org.openmole.core.workflow.task.TaskExecutionContext
import org.openmole.core.workflow.tools.{ Name, ExceptionEvent }
import org.openmole.core.workspace.{ Workspace, ConfigurationLocation }
import org.openmole.tool.collection.OrderedSlidingList
import scala.concurrent.stm._
import org.openmole.core.tools.service._

import scala.ref.WeakReference

object Environment {
  val maxExceptionsLog = ConfigurationLocation("Environment", "MaxExceptionsLog")

  Workspace += (maxExceptionsLog, "1000")

  case class JobSubmitted(job: ExecutionJob) extends Event[Environment]
  case class JobStateChanged(job: ExecutionJob, newState: ExecutionState, oldState: ExecutionState) extends Event[Environment]
  case class ExceptionRaised(job: ExecutionJob, exception: Throwable, level: Level) extends Event[Environment] with ExceptionEvent
  case class MoleJobExceptionRaised(job: ExecutionJob, exception: Throwable, level: Level, moleJob: MoleJob) extends Event[Environment] with ExceptionEvent
  case class JobCompleted(job: ExecutionJob, log: RuntimeLog, info: RuntimeInfo) extends Event[Environment]

  case class RuntimeLog(beginTime: Long, executionBeginTime: Long, executionEndTime: Long, endTime: Long)
}

import Environment._

sealed trait Environment <: Name {
  private[execution] val _done = Ref(0L)
  private[execution] val _failed = Ref(0L)

  private lazy val _errors =
    new OrderedSlidingList[ExceptionEvent](
      Workspace.preferenceAsInt(maxExceptionsLog)
    )(Ordering.by[ExceptionEvent, Int](_.level.intValue()))

  def error(e: ExceptionEvent) = _errors += e
  def errors: List[ExceptionEvent] = _errors.values
  def readErrors: List[ExceptionEvent] = _errors.read()

  def submitted: Long
  def running: Long
  def done: Long = _done.single()
  def failed: Long = _failed.single()

}

trait SubmissionEnvironment <: Environment {
  def submit(job: Job)
  def jobs: Iterable[ExecutionJob]
}

object LocalEnvironment {

  val DefaultNumberOfThreads = new ConfigurationLocation("LocalExecutionEnvironment", "ThreadNumber")

  Workspace += (DefaultNumberOfThreads, "1")
  var defaultNumberOfThreads = Workspace.preferenceAsInt(DefaultNumberOfThreads)

  def apply(
    nbThreads:    Int            = defaultNumberOfThreads,
    deinterleave: Boolean        = false,
    name:         Option[String] = None
  ) = new LocalEnvironment(nbThreads, deinterleave, name)

}

class LocalEnvironment(
    val nbThreads:     Int,
    val deinterleave:  Boolean,
    override val name: Option[String]
) extends Environment {

  @transient lazy val pool = new ExecutorPool(nbThreads, WeakReference(this))

  def nbJobInQueue = pool.waiting

  def submit(job: Job, executionContext: TaskExecutionContext): Unit =
    submit(new LocalExecutionJob(executionContext, job.moleJobs, Some(job.moleExecution)))

  def submit(moleJob: MoleJob, executionContext: TaskExecutionContext): Unit =
    submit(new LocalExecutionJob(executionContext, List(moleJob), None))

  private def submit(ejob: LocalExecutionJob): Unit = {
    pool.enqueue(ejob)
    ejob.state = ExecutionState.SUBMITTED
    EventDispatcher.trigger(this, new Environment.JobSubmitted(ejob))
  }

  def submitted: Long = pool.waiting
  def running: Long = pool.running
}
