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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.implementation.data


import org.openmole.commons.exception.InternalProcessingError
import org.openmole.core.implementation.tools.CloningService
import org.openmole.core.implementation.tools.ContextBuffer
import org.openmole.core.implementation.tools.LevelComputing
import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.core.model.data.{IDataChannel,IPrototype,IDataSet,IData,IContext}
import org.openmole.core.model.job.{ITicket}
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.task.IGenericTask
import scala.collection.immutable.TreeSet
import scala.collection.mutable.ArraySeq
import scala.collection.mutable.ListBuffer

class DataChannel(val start: IGenericCapsule, val end:  IGenericCapsule, val variableNames: Set[String]) extends IDataChannel {

  start.plugOutputDataChannel(this)
  end.plugInputDataChannel(this)
  
  def this(start: IGenericCapsule, end: IGenericCapsule, head: String, variables: Array[String]) = {
    this(start, end, (ListBuffer(head) ++ variables).toSet[String])
  }

  def this(start: IGenericCapsule, end: IGenericCapsule, head: IPrototype[_], variables: Array[IPrototype[_]]) = {
    this(start, end, (ListBuffer(head) ++ variables).map( v => v.name).toSet)
  }

  def this(start: IGenericCapsule, end: IGenericCapsule, dataset: IDataSet) = {
    this(start, end, dataset.map( v => v.prototype.name ).toSet)
  }
   
  override def consums(ticket: ITicket, moleExecution: IMoleExecution): IContext = {
    val levelComputing = LevelComputing(moleExecution)
    val dataChannelRegistry = moleExecution.localCommunication.dataChannelRegistry

    val startLevel = levelComputing.level(start)
    val endLevel = levelComputing.level(end)

    var levelDif = endLevel - startLevel
    if (levelDif < 0) levelDif = 0
   
    var currentTicket = ticket

    for (i <- 0 until levelDif)  {
      currentTicket = currentTicket.parent match {
        case None => throw new InternalProcessingError("Bug no supposed to reach root ticket")
        case Some(p) => p
      }
    }

    if(endLevel <= startLevel)
      dataChannelRegistry.remove(this, currentTicket) match {
        case None => throw new InternalProcessingError("No context registred for data channel found in input of task " + end.toString)
        case Some(ctx) => ctx.toContext
      }
    else dataChannelRegistry.consult(this, currentTicket) match {
      case None => throw new InternalProcessingError("No context registred for data channel found in input of task " + end.toString)
      case Some(ctx) => ctx.toContext
    }

  }

  override def provides(fromContext: IContext, ticket: ITicket, toClone: Set[String], moleExecution: IMoleExecution) = synchronized {
    val levelComputing = LevelComputing(moleExecution)

    val startLevel = levelComputing.level(start)
    val endLevel = levelComputing.level(end)
    val toLowerLevel = endLevel < startLevel

    val dataChannelRegistry = moleExecution.localCommunication.dataChannelRegistry

    dataChannelRegistry.synchronized {
      if (!toLowerLevel) {
        val toContext = ContextBuffer(fromContext, toClone, variableNames)
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
            val ctx = new ContextBuffer(true)
            dataChannelRegistry.register(this, workingOnTicket, ctx)
            ctx
        }

        toContext ++= (fromContext, toClone, variableNames)
      }
      
      
    }
  }

  
  /*private def flatProvide(fromContext: IContext, data: Iterable[IData[_]], toContext: IContext) = {
    for (d <- data) {
      fromContext.variable(d.prototype) match {
        case Some(variable) => toContext += variable
        case None => 
      }
    }
  }
  
  private def arrayProvide(fromContext: IContext, data: Iterable[IData[_]], toContext: IContext) = {
    for(d <- data) {
      fromContext.value(d.prototype) match {
        case Some(curVal) =>
          toContext += {
            //FIXME downcasting and inefficient use of arrays
            val itProt = toArray(d.prototype).asInstanceOf[IPrototype[Array[Any]]]
            toContext.value(itProt) match {
              case None => 
                val collec = Array[Any](curVal)
                new Variable(itProt, collec)
              case Some(collec) =>
                new Variable(itProt, Array.concat(collec, Array[Any](curVal)))
            }
          } 
        case None =>
      }
    }
  
  }*/
  
  def data: Iterable[IData[_]] = {
    end.task match {
      case None => List.empty
      case Some(task) => (for (d <- task.inputs ; if (variableNames.contains(d.prototype.name))) yield d)
    }
  }
}
