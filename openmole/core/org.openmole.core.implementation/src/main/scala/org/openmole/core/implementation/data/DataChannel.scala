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
import org.openmole.core.implementation.tools.ClonningService
import org.openmole.core.implementation.tools.LevelComputing
import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.core.model.data.{IDataChannel,IPrototype,IDataSet,IData,IContext}
import org.openmole.core.model.job.{ITicket}
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.task.IGenericTask
import scala.collection.immutable.TreeSet
import scala.collection.mutable.ArraySeq
import scala.collection.mutable.ListBuffer
import org.openmole.core.implementation.data.Util._

class DataChannel(val start: IGenericCapsule, val end:  IGenericCapsule, val variableNames: Set[String]) extends IDataChannel {

  start.plugOutputDataChannel(this)
  end.plugInputDataChannel(this)
  
  def this(start: IGenericCapsule, end: IGenericCapsule, head: String, variables: Array[String]) = {
    this(start, end, (List(head) ++ variables).toSet[String])
  }

  def this(start: IGenericCapsule, end: IGenericCapsule, head: IPrototype[_], variables: Array[IPrototype[_]]) {
    this(start, end, (List(head) ++ variables).map( v => v.name).toSet)
  }

  def this(start: IGenericCapsule, end: IGenericCapsule, dataset: IDataSet) = {
    this(start, end, dataset.map( v => v.prototype.name).toSet)
  }
   
  override def consums(context: IContext, ticket: ITicket, moleExecution: IMoleExecution): (IContext, Set[String]) = {
    val levelComputing = LevelComputing.levelComputing(moleExecution)
    val dataChannelRegistry = moleExecution.localCommunication.dataChannelRegistry

    val startLevel = levelComputing.level(start)
    val endLevel = levelComputing.level(end)

    var levelDif = endLevel - startLevel
    if (levelDif < 0) {
      levelDif = 0
    }

    var currentTicket = ticket

    for (i <- 0 until levelDif)  {
      currentTicket = currentTicket.parent match {
        case None => throw new InternalProcessingError("Bug no supposed to reach root ticket")
        case Some(p) => p
      }
    }
    
    dataChannelRegistry.remove(this, currentTicket) match {
      case None => throw new InternalProcessingError("No context registred for data channel found in input of task " + end.toString)
      case Some(ctx) => (ctx, if (levelDif > 0) ctx.map{ _.prototype.name }.toSet else Set.empty) 
    }

  }

  override def provides(context: IContext, ticket: ITicket, toClone: Set[String], moleExecution: IMoleExecution) = synchronized {
    //IMoleExecution execution = context.getGlobalValue(IMole.WorkflowExecution);
    val levelComputing = LevelComputing.levelComputing(moleExecution)

    val startLevel = levelComputing.level(start)
    val endLevel = levelComputing.level(end)

    var workingOnTicket = ticket

    val array = endLevel < startLevel

    //If from higher level
    for (i <- startLevel until endLevel) {
      workingOnTicket = workingOnTicket.parent match {
        case None => throw new InternalProcessingError("Bug should never get to root.")
        case Some(p) => p
      }
    }

    val dataChannelRegistry = moleExecution.localCommunication.dataChannelRegistry

    dataChannelRegistry synchronized {
      val curentContext = dataChannelRegistry.consult(this, workingOnTicket) match {
        case Some(ctx) => ctx
        case None => 
          val ctx = new Context
          dataChannelRegistry.register(this, workingOnTicket, ctx)
          ctx
      }

      if (!array) {
        for (d <- data) {
          context.variable(d.prototype) match {
            case Some(variable) => 
              curentContext += (
                if(toClone.contains(variable.prototype.name))
                  ClonningService.clone(variable)
                else variable)
            case None => 
          }
        }
      } else {
        for(d <- data) {
          context.value(d.prototype) match {
            case Some(curVal) =>
              curentContext += {
                val itProt = toArray(d.prototype)
                curentContext.value(itProt) match {
                  case None => 
                    val collec = Vector(curVal)
                    //FIXME downcasting this is supposed to be ok without but is not
                    new Variable(itProt.asInstanceOf[IPrototype[Iterable[Any]]], collec)
                  case Some(collec) => 
                    //FIXME downcasting this is supposed to be ok without but is not
                    new Variable(itProt.asInstanceOf[IPrototype[Iterable[Any]]], collec ++ Vector(curVal))
                }
              } 
            case None =>
          }
        }
      }

    }
  }

  def data: Iterable[IData[_]] = {
    end.task match {
      case None => List.empty
      case Some(task) => (for (d <- task.inputs ; if (variableNames.contains(d.prototype.name))) yield d)
    }
  }
}
