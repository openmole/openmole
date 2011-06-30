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

package org.openmole.core.implementation.tools

import org.openmole.misc.exception.InternalProcessingError
import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.core.model.mole.IMole
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.transition.{IExplorationTransition,IAggregationTransition, IGenericTransition}
import scala.collection.mutable.HashSet
import scala.collection.mutable.WeakHashMap
import scala.collection.mutable.SynchronizedMap

object LevelComputing {

  val levelComputings = new WeakHashMap[IMoleExecution, LevelComputing] with SynchronizedMap[IMoleExecution, LevelComputing]

  def apply(moleExecution: IMoleExecution): LevelComputing = 
    levelComputings.getOrElseUpdate(moleExecution, new LevelComputing(moleExecution.mole))
    
}


class LevelComputing(mole: IMole) {
  
  @transient private val levelCache = new WeakHashMap[IGenericCapsule, Int]

  def level(capsule: IGenericCapsule): Int = synchronized {
    levelCache.getOrElse (capsule, {
        val l = level(capsule, new HashSet[IGenericCapsule], Int.MaxValue)
        if(l == Int.MaxValue) throw new InternalProcessingError("Error in level computing level could not be equal to MAXVALUE." + capsule.taskOrException.name)
        l
    })
  }
  
  private def level (capsule : IGenericCapsule, alreadySeen: HashSet[IGenericCapsule], lvl : Int): Int = {
    if (capsule.equals (mole.root)) return 1
    if (alreadySeen.contains(capsule)) return lvl
    capsule.intputSlots map (slot => {
        if (slot.transitions.size > 0)
          slot.transitions map (t => {
            val inLevel = levelCache.getOrElse (t.start, level (t.start, alreadySeen + capsule, Int.MaxValue))
            t match {
              case _ : IExplorationTransition => inLevel + 1
              case _ : IAggregationTransition => inLevel - 1
              case _ => inLevel
            }
          }) min
        else Int.MaxValue
    }) min
  }
}
