/*
 * Copyright (C) 2012 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

package org.openmole.core.implementation.validation

import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.mole.IMole
import TypeUtil.receivedTypes
import org.openmole.core.model.data.DataModeMask._
import scala.collection.immutable.TreeMap
import org.openmole.core.model.task.IMoleTask
import org.openmole.misc.tools.obj.ClassUtils._
import DataflowProblem._
import TopologyProblem._
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Queue
import org.openmole.core.implementation.tools.LevelComputing._

object Validation {
  
  def allMoles(mole: IMole) =
    mole ::
      mole.capsules.flatMap {
        _.task match {
          case Some(task) => 
            task match {
              case mt: IMoleTask => Some(mt.mole)
              case _ => None
            }
          case _ => None
        }
      }.toList
  
  def typeErrors(mole: IMole): Iterable[DataflowProblem] = 
    mole.capsules.flatMap {
      c => c.intputSlots.map {
        s => (c, s, TreeMap(receivedTypes(s).map{p => p.name -> p}.toSeq: _*), 
              c.task match {
            case Some(t) => t.parameters.map {
                p => p.variable.prototype.name -> p.variable.prototype
              }.toMap[String, IPrototype[_]]
            case None => Map.empty[String, IPrototype[_]]
          })
      }.flatMap {
        case(capsule, slot, received, parameters) =>
          capsule.inputs.filterNot(_.mode is optional).flatMap(
            input => 
            received.get(input.prototype.name) match {
              case Some(recieved) => 
                if(!input.prototype.isAssignableFrom(recieved)) Some(new WrongType(capsule, slot, input, recieved))
                else None
              case None => 
                parameters.get(input.prototype.name) match {
                  case Some(proto) => 
                    if(!input.prototype.isAssignableFrom(proto)) Some(new WrongType(capsule, slot, input, proto))
                    else None
                  case None => Some(new MissingInput(capsule, slot, input))
                }
            }
          )
      }
    }

  def topologyErrors(mole: IMole) = {
    val errors = new ListBuffer[TopologyProblem]
    val seen = new HashMap[ICapsule, (List[(List[ICapsule], Int)])]
    val toProcess = new Queue[(ICapsule, Int, List[ICapsule])]
    
    toProcess.enqueue((mole.root, 0, List.empty))
    seen(mole.root) = List((List.empty -> 0))
    
    while(!toProcess.isEmpty) {
      val (capsule, level, path) = toProcess.dequeue
      
      nextCaspules(capsule, level).foreach {
        case (nCap, nLvl) => 
          if(!seen.contains(nCap)) toProcess.enqueue((nCap, nLvl, capsule :: path))
          seen(nCap) = ((capsule :: path) -> nLvl) :: seen.getOrElse(nCap, List.empty)
      }
    }
    
    seen.filter{case (caps, paths) => paths.map{case(path, level) => level}.distinct.size > 1}.map {
      case(caps, paths) => new LevelProblem(caps, paths)
    }
  }
  
  def duplicatedTransitions(mole: IMole) =
    mole.capsules.flatMap {
      end => 
      end.intputSlots.flatMap {
        slot => 
        slot.transitions.toList.map{t => t.start -> t}.groupBy{case(c, t) => c}.filter{
          case(_, transitions) => transitions.size > 1
        }.map{
          case(_, transitions) => transitions.map{case(_, t) => t}
        }
      }
    }.map { t => new DuplicatedTransition(t)}
  

  def apply(mole: IMole) = 
    allMoles(mole).flatMap {
      m =>
        typeErrors(m) ++ 
        topologyErrors(m) ++ 
        duplicatedTransitions(m)
    }
  
}
