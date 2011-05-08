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

package org.openmole.core.implementation.tools

import java.util.logging.Logger
import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.mole.IMoleExecution
import scala.collection.mutable.HashSet
import scala.collection.mutable.WeakHashMap

object MutableComputing {

  object Mutable extends Enumeration {
    type Mutable = Value
    val Yes, YesForHigherLevel, No = Value
  }

  
  val mutableComputings = new WeakHashMap[IMoleExecution, MutableComputing]

  def apply(moleExecution: IMoleExecution): MutableComputing = synchronized {
    mutableComputings.get(moleExecution) match {
      case None =>  
        val ret = new MutableComputing
        mutableComputings.put(moleExecution, ret)
        ret
      case Some(ret) => ret
    }
  }
}


class MutableComputing {
  
  import MutableComputing.Mutable._
  
  @transient private val mutableCache = new WeakHashMap[(IGenericCapsule, IPrototype[_]), Mutable]
  
  def mutable(capsule: IGenericCapsule, prototype: IPrototype[_], levelComputing: LevelComputing): Mutable = synchronized {
    mutableCache.get(capsule, prototype) match {
      case Some(cachedMutable) => cachedMutable
      case None => 
        val l = mutableOfNextCapsules(capsule, prototype, 0, new HashSet[IGenericCapsule], levelComputing)
        mutableCache.put((capsule, prototype), l)
        l
    }
  }
  
  
  private def mutable(capsule: IGenericCapsule, prototype: IPrototype[_], relativeLevel: Int, allreadySeen: HashSet[IGenericCapsule], levelComputing: LevelComputing): Mutable = {
  //  Logger.getLogger(classOf[MutableComputing].getName).info(capsule.toString)
    
    if(allreadySeen.contains(capsule)) return No
    allreadySeen.add(capsule)

    capsule.task match {
      case None => return No
      case Some(task) => task.inputs(prototype.name) match {
          case None => return No
          case Some(i) => 
            if(i.mode.isMutable) {
              if(relativeLevel > 0) return YesForHigherLevel
              else Yes
            } else task.outputs(prototype.name) match {
              case None => return No
              case Some(o) => mutableOfNextCapsules(capsule, prototype, relativeLevel, allreadySeen, levelComputing)  
            }
        }
    }
    
  }
  
  
  private def mutableOfNextCapsules(capsule: IGenericCapsule, prototype: IPrototype[_], relativeLevel: Int, allreadySeen: HashSet[IGenericCapsule], levelComputing: LevelComputing): Mutable = {
    val startCapsuleLevel = levelComputing.level(capsule)

    var res = No
    for (t <- capsule.outputTransitions) {
      val newRelativeLevel = levelComputing.level(t.end.capsule) - startCapsuleLevel + relativeLevel
      mutable(t.end.capsule, prototype, newRelativeLevel, allreadySeen, levelComputing) match {
        case No =>
        case Yes => if(res == No) res = Yes
        case YesForHigherLevel => return YesForHigherLevel
      } 
    }
    for (c <- capsule.outputDataChannels) {
      val newRelativeLevel = levelComputing.level(c.end) - startCapsuleLevel + relativeLevel
      
      mutable(c.end, prototype, newRelativeLevel, allreadySeen, levelComputing) match {
        case No =>
        case Yes => if(res == No) res = Yes
        case YesForHigherLevel => return YesForHigherLevel
      } 
    }
    return res
  }
}
