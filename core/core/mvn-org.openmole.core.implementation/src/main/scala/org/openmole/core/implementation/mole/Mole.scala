/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.implementation.mole

import org.openmole.core.model.data._
import org.openmole.core.model.mole._
import org.openmole.core.model.task._
import org.openmole.core.model.transition._
import org.openmole.misc.exception.UserBadDataError
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer

object Mole {

  def nextCaspules(mole: IMole)(from: ICapsule, lvl: Int) =
    nextTransitions(mole)(from, lvl).map { case (t, lvl) ⇒ t.end.capsule -> lvl }

  def nextTransitions(mole: IMole)(from: ICapsule, lvl: Int) =
    mole.outputTransitions(from).map {
      case t: IAggregationTransition ⇒ t -> (lvl - 1)
      case t: IEndExplorationTransition ⇒ t -> (lvl - 1)
      case t: ISlaveTransition ⇒ t -> lvl
      case t: IExplorationTransition ⇒ t -> (lvl + 1)
      case t: ITransition ⇒ t -> lvl
    }

  def levels(mole: IMole) = {
    val cache = HashMap(mole.root -> 0)
    val toProceed = ListBuffer(mole.root -> 0)

    while (!toProceed.isEmpty) {
      val (capsule, level) = toProceed.remove(0)
      nextCaspules(mole)(capsule, level).foreach {
        case (c, l) ⇒
          val continue = !cache.contains(c)
          val lvl = cache.getOrElseUpdate(c, l)
          if (lvl != l) throw new UserBadDataError("Inconsistent level found for capsule " + c)
          if (continue) toProceed += (c -> l)
      }
    }
    cache
  }

}

class Mole(
    val root: ICapsule,
    val transitions: Iterable[ITransition] = Iterable.empty,
    val dataChannels: Iterable[IDataChannel] = Iterable.empty) extends IMole {

  lazy val levels = Mole.levels(this)
  def level(c: ICapsule) = levels(c)
}
