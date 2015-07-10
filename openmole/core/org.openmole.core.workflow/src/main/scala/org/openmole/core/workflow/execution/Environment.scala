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
import org.openmole.core.event.{ EventAccumulator, Event }
import org.openmole.core.workflow.job.Job
import org.openmole.core.workflow.job.MoleJob
import ExecutionState._
import org.openmole.core.workflow.tools.{ Name, ExceptionEvent }
import org.openmole.core.workspace.{ Workspace, ConfigurationLocation }
import scala.concurrent.stm._
import org.openmole.core.tools.service._

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

trait Environment <: Name {
  private[execution] val _done = Ref(0L)
  private[execution] val _failed = Ref(0L)

  private def _errors = Ref(List[ExceptionEvent]())

  def error(e: ExceptionEvent) = atomic { implicit ctx ⇒
    val max = Workspace.preferenceAsInt(maxExceptionsLog)
    _errors() = e :: _errors()
    if (_errors().size > max) _errors() = _errors().take(max)
  }

  def errors = _errors.single().reverse
  def readErrors = atomic { implicit ctx ⇒
    val errs = errors
    _errors() = List()
    errs
  }

  def submitted: Long
  def running: Long
  def done: Long = _done.single()
  def failed: Long = _failed.single()

  def submit(job: Job)
}
