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

package org.openmole.core.model.mole

import org.openmole.core.model.job.State.State
import org.openmole.core.model.tools.{ ExceptionEvent, IRegistryWithTicket }
import org.openmole.misc.eventdispatcher.Event
import java.util.logging.Level
import org.openmole.core.model.data.Context
import org.openmole.core.model.data.IDataChannel
import org.openmole.core.model.data.Variable
import org.openmole.core.model.job.IJob
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.job.MoleJobId
import scala.collection.mutable.Buffer
import org.openmole.core.model.execution.Environment

object IMoleExecution {

  class Starting extends Event[IMoleExecution]
  class Finished extends Event[IMoleExecution]
  case class JobStatusChanged(moleJob: IMoleJob, capsule: ICapsule, newState: State, oldState: State) extends Event[IMoleExecution]
  case class JobCreated(moleJob: IMoleJob, capsule: ICapsule) extends Event[IMoleExecution]
  case class JobSubmitted(moleJob: IJob, capsule: ICapsule, environment: Environment) extends Event[IMoleExecution]
  case class JobFinished(moleJob: IMoleJob, capsule: ICapsule) extends Event[IMoleExecution]
  case class JobFailed(moleJob: IMoleJob, capsule: ICapsule, exception: Throwable) extends Event[IMoleExecution] with ExceptionEvent {
    def level = Level.SEVERE
  }
  case class ExceptionRaised(moleJob: IMoleJob, exception: Throwable, level: Level) extends Event[IMoleExecution] with ExceptionEvent
  case class SourceExceptionRaised(source: ISource, capsule: ICapsule, exception: Throwable, level: Level) extends Event[IMoleExecution] with ExceptionEvent
  case class HookExceptionRaised(hook: IHook, moleJob: IMoleJob, exception: Throwable, level: Level) extends Event[IMoleExecution] with ExceptionEvent
  case class ProfilerExceptionRaised(profiler: Profiler, moleJob: IMoleJob, exception: Throwable, level: Level) extends Event[IMoleExecution] with ExceptionEvent

}

trait IMoleExecution {

  def started: Boolean

  def start: this.type
  def cancel: this.type
  def waitUntilEnded: this.type
  def finished: Boolean
  def exceptions: Iterable[Throwable]

  def mole: IMole
  def hooks: Hooks
  def sources: Sources
  def profiler: Profiler
  def implicits: Context

  def dataChannelRegistry: IRegistryWithTicket[IDataChannel, Buffer[Variable[_]]]

  def moleJobs: Iterable[IMoleJob]
  def id: String
  def nextTicket(parent: ITicket): ITicket
}

