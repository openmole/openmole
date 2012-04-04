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
import org.openmole.core.implementation.tools.LevelComputing
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.data.{IDataChannel,IPrototype,IDataSet,IData,IContext, IVariable}
import org.openmole.core.model.mole.ITicket
import org.openmole.core.model.mole.IMoleExecution
import scala.collection.mutable.ListBuffer

object DataChannel {
  def levelDelta(dataChannel: IDataChannel): Int = LevelComputing.levelDelta(dataChannel.start, dataChannel.end.capsule)
}

class DataChannel(
  val start: ICapsule,
  val end:  ISlot, 
  val filtered: Set[String]) extends IDataChannel {

  start.addOutputDataChannel(this)
  end.addInputDataChannel(this)
  
  def this(start: ICapsule, end: ISlot) = this(start, end, Set.empty[String])
 
  def this(start: ICapsule, end: ICapsule) = this(start, end.defaultInputSlot, Set.empty[String])
  
  def this(start: ICapsule, end: ISlot, head: String, variables: Array[String] = Array.empty) = 
    this(start, end, (ListBuffer(head) ++ variables).toSet[String])

  def this(start: ICapsule, end: ISlot, head: IPrototype[_], variables: Array[IPrototype[_]]) =
    this(start, end, (ListBuffer(head) ++ variables).map( v => v.name).toSet)

  def this(start: ICapsule, end: ISlot, head: IPrototype[_]) = this(start, end, head, Array.empty[IPrototype[_]])

  def this(start: ICapsule, end: ISlot, dataset: IDataSet) = this(start, end, dataset.map( _.prototype.name ).toSet)

  def this(start: ICapsule, end: ICapsule, head: String, variables: Array[String]) = 
    this(start, end.defaultInputSlot, (ListBuffer(head) ++ variables).toSet[String])

  def this(start: ICapsule, end: ICapsule, dataset: IDataSet) = this(start, end.defaultInputSlot, dataset.map(_.prototype.name ).toSet)
 
  def this(start: ICapsule, end: ISlot, prototypes: Array[IPrototype[_]]) = this(start, end, prototypes.map(_.name).toSet)
  
  def this(start: ICapsule, end: ICapsule, prototype: Array[IPrototype[_]]) = this(start, end.defaultInputSlot, prototype)
  
  
  override def consums(ticket: ITicket, moleExecution: IMoleExecution): Iterable[IVariable[_]] = moleExecution.synchronized {
    val levelDelta = LevelComputing(moleExecution).levelDelta(start, end.capsule)
    val dataChannelRegistry = moleExecution.dataChannelRegistry
    
    { if(levelDelta <= 0) dataChannelRegistry.remove(this, ticket).getOrElse(new ListBuffer[IVariable[_]])
      else {
        val workingOnTicket = (0 until levelDelta).foldLeft(ticket) {
          (c, e) => c.parent.getOrElse(throw new InternalProcessingError("Bug should never get to root."))
        }
        dataChannelRegistry.consult(this, workingOnTicket) getOrElse(new ListBuffer[IVariable[_]])
      }
    }.toIterable
  }

  override def provides(fromContext: IContext, ticket: ITicket, moleExecution: IMoleExecution) = moleExecution.synchronized {
    val levelDelta = LevelComputing(moleExecution).levelDelta(start, end.capsule)
    val dataChannelRegistry = moleExecution.dataChannelRegistry
    if (levelDelta >= 0) {
      val toContext = ListBuffer() ++ fromContext.filterNot(v => filtered.contains(v.prototype.name))
      dataChannelRegistry.register(this, ticket, toContext)
    } else {
      val workingOnTicket = (levelDelta until 0).foldLeft(ticket) {
        (c, e) => c.parent.getOrElse(throw new InternalProcessingError("Bug should never get to root."))
      }
        
      val toContext = dataChannelRegistry.getOrElseUpdate(this, workingOnTicket, new ListBuffer[IVariable[_]]) 
      toContext ++= fromContext.filterNot(v => filtered.contains(v.prototype.name))
    }  
    
  }
  
  def data = start.outputs.filterNot(d => filtered.contains(d.prototype.name))
    
}
