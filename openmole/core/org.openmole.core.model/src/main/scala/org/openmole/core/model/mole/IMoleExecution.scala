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
import org.openmole.core.model.tools.IRegistryWithTicket
import org.openmole.misc.eventdispatcher.Event
import java.util.logging.Level
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IDataChannel
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.job.IJob
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.job.MoleJobId
import scala.collection.mutable.Buffer

object IMoleExecution {
  case class Starting extends Event[IMoleExecution]
  case class Finished extends Event[IMoleExecution]
  case class OneJobStatusChanged(val moleJob: IMoleJob, val newState: State, val oldState: State) extends Event[IMoleExecution]
  case class OneJobSubmitted(val moleJob: IMoleJob) extends Event[IMoleExecution]
  case class JobInCapsuleFinished(val moleJob: IMoleJob, val capsule: ICapsule) extends Event[IMoleExecution]
  case class JobInCapsuleStarting(val moleJob: IMoleJob, val capsule: ICapsule) extends Event[IMoleExecution]
  case class ExceptionRaised(val moleJob: IMoleJob, val exception: Throwable, level: Level) extends Event[IMoleExecution]
}

trait IMoleExecution {

  def started: Boolean

  def start: this.type
  def cancel: this.type
  def waitUntilEnded: this.type
  def finished: Boolean
  def exceptions: Iterable[Throwable]

  def mole: IMole

  def dataChannelRegistry: IRegistryWithTicket[IDataChannel, Buffer[IVariable[_]]]

  def moleJobs: Iterable[IMoleJob]
  def id: String
  def nextTicket(parent: ITicket): ITicket
}

