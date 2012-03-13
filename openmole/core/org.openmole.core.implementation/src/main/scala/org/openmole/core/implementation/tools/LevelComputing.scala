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

import org.openmole.misc.exception.UserBadDataError
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.transition.{ITransition,IAggregationTransition, IExplorationTransition, IEndExplorationTransition}
import scala.collection.mutable.WeakHashMap
import scala.collection.mutable.SynchronizedMap
import scala.collection.mutable.ListBuffer

object LevelComputing {

  val levelComputings = new WeakHashMap[IMoleExecution, LevelComputing] with SynchronizedMap[IMoleExecution, LevelComputing]

  def apply(moleExecution: IMoleExecution): LevelComputing = 
    levelComputings.getOrElseUpdate(moleExecution, new LevelComputing(moleExecution.mole.root))
    
  def levelDelta(from: ICapsule, to: ICapsule): Int = {
    val cache = WeakHashMap(from -> 0)
    val toProceed = ListBuffer(from -> 0)
    
    while(!toProceed.isEmpty) {
      val proceed = toProceed.remove(0)
      nextCaspules(proceed._1, proceed._2).foreach {
        case(c, l) =>
          if(c == to) return l
          val continue = !cache.contains(c)
          val lvl = cache.getOrElseUpdate(c, l)
          if(lvl != l) throw new UserBadDataError("Inconsistent level found for capsule " + c)
          if(continue) toProceed += (c -> l)
      }
    }
    throw new UserBadDataError("No connection found from capsule " + from + " to capsule " + to)
  }
  
  def nextCaspules(from: ICapsule, lvl: Int) =
    nextTransitions(from, lvl).map{ case (t, lvl) => t.end.capsule -> lvl }
   
  def nextTransitions(from: ICapsule, lvl: Int) = 
    from.outputTransitions.map {
      case t: IAggregationTransition => t -> (lvl - 1)
      case t: IEndExplorationTransition => t -> (lvl - 1)
      case t: IExplorationTransition => t -> (lvl + 1)
      case t: ITransition => t -> lvl
    }
  
  
}

class LevelComputing(root: ICapsule) {
  
  import LevelComputing._

  @transient private val levelCache = {
    val cache = WeakHashMap(root -> 0)
    val toProceed = ListBuffer(root -> 0)
    
    while(!toProceed.isEmpty) {
      val proceed = toProceed.remove(0)
      nextCaspules(proceed._1, proceed._2).foreach {
        case(c, l) =>
          val continue = !cache.contains(c)
          val lvl = cache.getOrElseUpdate(c, l)
          if(lvl != l) throw new UserBadDataError("Inconsistent level found for capsule " + c)
          if(continue) toProceed += (c -> l)
      }
    }
    cache
  }
  
  def levelDelta(from: ICapsule, to: ICapsule) = level(to) - level(from)
  
  def level(capsule: ICapsule) = levelCache.getOrElse(capsule, throw new UserBadDataError("Capsule " + capsule + " not connected to the mole."))
 
}
