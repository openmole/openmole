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


import org.openmole.misc.exception.InternalProcessingError
import org.openmole.core.implementation.tools.VariablesBuffer
import org.openmole.core.implementation.tools.LevelComputing
import org.openmole.core.implementation.mole.Capsule._
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.data.{IDataChannel,IPrototype,IDataSet,IData,IContext, IVariable}
import org.openmole.core.model.mole.ITicket
import org.openmole.core.model.mole.IMoleExecution
import scala.collection.mutable.ListBuffer

class DataChannel(val start: ICapsule, val end:  ICapsule, val variableNames: Set[String]) extends IDataChannel {

  start.addOutputDataChannel(this)
  end.addInputDataChannel(this)
  
  def this(start: ICapsule, end: ICapsule, head: String, variables: Array[String]) = 
    this(start, end, (ListBuffer(head) ++ variables).toSet[String])

  def this(start: ICapsule, end: ICapsule, head: IPrototype[_], variables: Array[IPrototype[_]]) =
    this(start, end, (ListBuffer(head) ++ variables).map( v => v.name).toSet)

  def this(start: ICapsule, end: ICapsule, head: IPrototype[_]) = this(start, end, head, Array.empty[IPrototype[_]])

  def this(start: ICapsule, end: ICapsule, dataset: IDataSet) = this(start, end, dataset.map( v => v.prototype.name ).toSet)
   
  override def consums(ticket: ITicket, moleExecution: IMoleExecution): Iterable[IVariable[_]] = {
    val levelComputing = LevelComputing(moleExecution)
    val dataChannelRegistry = moleExecution.dataChannelRegistry

    val startLevel = levelComputing.level(start)
    val endLevel = levelComputing.level(end)

    var levelDif = endLevel - startLevel
    if (levelDif < 0) levelDif = 0
   
    var currentTicket = ticket

    for (i <- 0 until levelDif) {
      currentTicket = currentTicket.parent match {
        case None => throw new InternalProcessingError("Bug no supposed to reach root ticket")
        case Some(p) => p
      }
    }
    {
      if(endLevel <= startLevel) dataChannelRegistry.remove(this, currentTicket) getOrElse(new VariablesBuffer)
      else dataChannelRegistry.consult(this, currentTicket) getOrElse(new VariablesBuffer)
    }.toIterable
  }

  override def provides(fromContext: IContext, ticket: ITicket, moleExecution: IMoleExecution) = synchronized {
    val levelComputing = LevelComputing(moleExecution)

    val startLevel = levelComputing.level(start)
    val endLevel = levelComputing.level(end)
    val toLowerLevel = endLevel < startLevel

    val dataChannelRegistry = moleExecution.dataChannelRegistry

    dataChannelRegistry.synchronized {
      if (!toLowerLevel) {
        val toContext = VariablesBuffer(fromContext.filter(v => variableNames.contains(v.prototype.name)))
        dataChannelRegistry.register(this, ticket, toContext)
      }
      else {
        var workingOnTicket = ticket
        for (i <- startLevel until endLevel) {
          workingOnTicket = workingOnTicket.parent match {
            case None => throw new InternalProcessingError("Bug should never get to root.")
            case Some(p) => p
          }
        }
        
        val toContext = dataChannelRegistry.consult(this, workingOnTicket) match {
          case Some(ctx) => ctx
          case None => 
            val ctx = new VariablesBuffer
            dataChannelRegistry.register(this, workingOnTicket, ctx)
            ctx
        }

        toContext ++= fromContext.filter(v => variableNames.contains(v.prototype.name))
      }  
    }
  }
  
  def data: Iterable[IData[_]] =
    (for (d <- start.outputs ; if (variableNames.contains(d.prototype.name))) yield d)
    
}
