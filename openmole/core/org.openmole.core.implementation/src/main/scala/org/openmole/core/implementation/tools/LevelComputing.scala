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
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.mole.IMole
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.transition.{ITransition,IAggregationTransition, IExplorationTransition}
import scala.annotation.tailrec
import scala.collection.immutable.HashSet
import scala.collection.mutable.WeakHashMap
import scala.collection.mutable.SynchronizedMap

object LevelComputing {

  val levelComputings = new WeakHashMap[IMole, LevelComputing] with SynchronizedMap[IMole, LevelComputing]

  def apply(mole: IMole): LevelComputing = 
    levelComputings.getOrElseUpdate(mole, new LevelComputing(mole.root))
    
  def levelDelta(from: ICapsule, to: ICapsule): Int = levelDelta(from, to, HashSet.empty)
  
  private def levelDelta(from: ICapsule, to: ICapsule, alreadySeen: HashSet[ICapsule]): Int = {
    if(alreadySeen.contains(from)) return Int.MaxValue
    if (from == to) 0
    else {
      val newAlreadySeen = alreadySeen + from
      from.outputTransitions.map {
        case t: IAggregationTransition => levelDelta(t.end.capsule, to, newAlreadySeen) - 1
        case t: IExplorationTransition => levelDelta(t.end.capsule, to, newAlreadySeen) + 1
        case t: ITransition => levelDelta(t.end.capsule, to, newAlreadySeen)
      }.min
    }
  }
}


class LevelComputing(root: ICapsule) {
  
  @transient private val levelCache = new WeakHashMap[ICapsule, Int]

  def levelDelta(from: ICapsule, to: ICapsule) = level(to) - level(from)
  
  def level(capsule: ICapsule): Int = synchronized {
    levelCache.getOrElse (capsule, {
        val l = level(capsule, HashSet.empty)
        if(l == Int.MaxValue) throw new InternalProcessingError("Error in level computing level could not be equal to MAXVALUE." + capsule.taskOrException.name)
        l
      })
  }
  
  private def level (capsule : ICapsule, alreadySeen: HashSet[ICapsule]): Int = {
    if (capsule.equals (root)) return 1
    if (alreadySeen.contains(capsule)) return Int.MaxValue
    capsule.intputSlots map (slot => {
        if (slot.transitions.size > 0)
          slot.transitions map (t => {
              val inLevel = levelCache.getOrElse (t.start, level (t.start, alreadySeen + capsule))
              t match {
                case _ : IExplorationTransition => inLevel + 1
                case _ : IAggregationTransition => inLevel - 1
                case t => inLevel
              }
            }) min
        else Int.MaxValue
      }) min
  }
}
