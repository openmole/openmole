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

import org.openmole.commons.exception.InternalProcessingError
import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.core.model.mole.IMole
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.transition.{IExplorationTransition,IAggregationTransition, IGenericTransition}
import scala.collection.mutable.HashSet
import scala.collection.mutable.WeakHashMap

object LevelComputing {

  val levelComputings = new WeakHashMap[IMoleExecution, LevelComputing]

  def levelComputing(moleExecution: IMoleExecution): LevelComputing = synchronized {
    levelComputings.get(moleExecution) match {
      case None =>  
        val ret = new LevelComputing(moleExecution.mole)
        levelComputings.put(moleExecution, ret)
        ret
      case Some(ret) => ret
    }
  }
}


class LevelComputing(mole: IMole) {
  
  @transient
  private val levelCache = new WeakHashMap[IGenericCapsule, Int]


  //TODO derecurisivate
  def level(capsule: IGenericCapsule): Int = {

    levelCache.get(capsule) match {
      case Some(cachedLevel) => cachedLevel
      case None => 
        val l = level(capsule, new HashSet[IGenericCapsule])
        if(l == Int.MaxValue) throw new InternalProcessingError("Error in level computing level could not be equal to MAXVALUE.")
        levelCache.put(capsule, l)
        l
    }
  }

  def level(capsule: IGenericCapsule, allreadySeen: HashSet[IGenericCapsule]): Int = {
    if (allreadySeen.contains(capsule)) {
      return Int.MaxValue
    } else {
      allreadySeen.add(capsule)
    }

    if (capsule.equals(mole.root)) return 1
        

    var minLevel = Int.MaxValue

    for (slot <- capsule.intputSlots) {
      for (t <- slot.transitions) {
        var inLevel = level(t.start, allreadySeen)

        if (classOf[IExplorationTransition].isAssignableFrom(t.getClass)) {
          inLevel += 1
        } else if (classOf[IAggregationTransition].isAssignableFrom(t.getClass)) {
          inLevel -= 1
        }

        if (inLevel < minLevel) minLevel = inLevel
                
      }
    }

    return minLevel
  }
}
