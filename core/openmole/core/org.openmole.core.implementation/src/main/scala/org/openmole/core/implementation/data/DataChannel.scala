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

package org.openmole.core.implementation.data

import org.openmole.core.model.transition._
import org.openmole.misc.exception._
import org.openmole.core.implementation.mole._
import org.openmole.core.implementation.puzzle._
import org.openmole.core.implementation.tools._
import org.openmole.core.model.data._
import org.openmole.core.model.mole._
import org.openmole.core.model.tools._
import scala.collection.mutable.ListBuffer

object DataChannel {
  def levelDelta(mole: IMole)(dataChannel: IDataChannel): Int =
    mole.level(dataChannel.end.capsule) - mole.level(dataChannel.start)
}

class DataChannel(
    val start: ICapsule,
    val end: Slot,
    val filter: Filter[String]) extends IDataChannel {

  def this(start: ICapsule, end: Slot, filtered: String*) = this(start, end, Block(filtered: _*))

  override def consums(ticket: ITicket, moleExecution: IMoleExecution): Iterable[Variable[_]] = moleExecution.synchronized {
    val delta = levelDelta(moleExecution.mole)
    val dataChannelRegistry = moleExecution.dataChannelRegistry

    {
      if (delta <= 0) dataChannelRegistry.remove(this, ticket).getOrElse(new ListBuffer[Variable[_]])
      else {
        val workingOnTicket = (0 until delta).foldLeft(ticket) {
          (c, e) ⇒ c.parent.getOrElse(throw new InternalProcessingError("Bug should never get to root."))
        }
        dataChannelRegistry.consult(this, workingOnTicket) getOrElse (new ListBuffer[Variable[_]])
      }
    }.toIterable
  }

  override def provides(fromContext: Context, ticket: ITicket, moleExecution: IMoleExecution) = moleExecution.synchronized {
    val delta = levelDelta(moleExecution.mole)
    val dataChannelRegistry = moleExecution.dataChannelRegistry

    if (delta >= 0) {
      val toContext = ListBuffer() ++ fromContext.values.filterNot(v ⇒ filter(v.prototype.name))
      dataChannelRegistry.register(this, ticket, toContext)
    } else {
      val workingOnTicket = (delta until 0).foldLeft(ticket) {
        (c, e) ⇒ c.parent.getOrElse(throw new InternalProcessingError("Bug should never get to root."))
      }
      val toContext = dataChannelRegistry.getOrElseUpdate(this, workingOnTicket, new ListBuffer[Variable[_]])
      toContext ++= fromContext.values.filterNot(v ⇒ filter(v.prototype.name))
    }
  }

  def data(mole: IMole, sources: Sources, hooks: Hooks) =
    start.outputs(mole, sources, hooks).filterNot(d ⇒ filter(d.prototype.name))

  def levelDelta(mole: IMole): Int = DataChannel.levelDelta(mole)(this)

  override def toString = "DataChannel from " + start + " to " + end

}
