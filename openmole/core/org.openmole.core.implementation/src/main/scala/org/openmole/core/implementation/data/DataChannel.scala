/*
 * Copyright (C) 2010 reuillon
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

import org.openmole.core.model.transition.ISlot
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.core.implementation.mole._
import org.openmole.core.implementation.puzzle._
import org.openmole.core.implementation.tools.LevelComputing
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.data._
import org.openmole.core.model.mole._
import scala.collection.mutable.ListBuffer

object DataChannel {
  //def apply(p: Puzzle) = new DataChannel(p.first.capsule, p.lasts)
  def levelDelta(dataChannel: IDataChannel): Int = LevelComputing.levelDelta(dataChannel.start, dataChannel.end.capsule)
}

class DataChannel(
    val start: ICapsule,
    val end: ISlot,
    val filtered: String*) extends IDataChannel {

  start.addOutputDataChannel(this)
  end.addInputDataChannel(this)

  @transient lazy val filteredSet = filtered.toSet

  override def consums(ticket: ITicket, moleExecution: IMoleExecution): Iterable[IVariable[_]] = moleExecution.synchronized {
    val levelDelta = LevelComputing(moleExecution).levelDelta(start, end.capsule)
    val dataChannelRegistry = moleExecution.dataChannelRegistry

    {
      if (levelDelta <= 0) dataChannelRegistry.remove(this, ticket).getOrElse(new ListBuffer[IVariable[_]])
      else {
        val workingOnTicket = (0 until levelDelta).foldLeft(ticket) {
          (c, e) ⇒ c.parent.getOrElse(throw new InternalProcessingError("Bug should never get to root."))
        }
        dataChannelRegistry.consult(this, workingOnTicket) getOrElse (new ListBuffer[IVariable[_]])
      }
    }.toIterable
  }

  override def provides(fromContext: IContext, ticket: ITicket, moleExecution: IMoleExecution) = moleExecution.synchronized {
    val levelDelta = LevelComputing(moleExecution).levelDelta(start, end.capsule)
    val dataChannelRegistry = moleExecution.dataChannelRegistry
    if (levelDelta >= 0) {
      val toContext = ListBuffer() ++ fromContext.values.filterNot(v ⇒ filteredSet.contains(v.prototype.name))
      dataChannelRegistry.register(this, ticket, toContext)
    } else {
      val workingOnTicket = (levelDelta until 0).foldLeft(ticket) {
        (c, e) ⇒ c.parent.getOrElse(throw new InternalProcessingError("Bug should never get to root."))
      }

      val toContext = dataChannelRegistry.getOrElseUpdate(this, workingOnTicket, new ListBuffer[IVariable[_]])
      toContext ++= fromContext.values.filterNot(v ⇒ filteredSet.contains(v.prototype.name))
    }

  }

  def data = start.outputs.filterNot(d ⇒ filteredSet.contains(d.prototype.name))

}
