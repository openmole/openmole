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

package org.openmole.core.workflow.mole

import org.openmole.core.context.PrototypeSet
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.workflow.transition._

import scala.collection._

object Mole {

  def nextCapsules(mole: Mole)(from: Capsule, lvl: Int) =
    nextTransitions(mole)(from, lvl).map { case (t, lvl) ⇒ t.end.capsule → lvl }

  def nextTransitions(mole: Mole)(from: Capsule, lvl: Int) =
    mole.outputTransitions(from).map {
      case t: IAggregationTransition    ⇒ t → (lvl - 1)
      case t: IEndExplorationTransition ⇒ t → (lvl - 1)
      case t: ISlaveTransition          ⇒ t → lvl
      case t: IExplorationTransition    ⇒ t → (lvl + 1)
      case t: ITransition               ⇒ t → lvl
    }

  def levels(mole: Mole) = {
    val cache = mutable.HashMap(mole.root → 0)
    val toProceed = mutable.ListBuffer(mole.root → 0)

    while (!toProceed.isEmpty) {
      val (capsule, level) = toProceed.remove(0)
      nextCapsules(mole)(capsule, level).foreach {
        case (c, l) ⇒
          val continue = !cache.contains(c)
          val lvl = cache.getOrElseUpdate(c, l)
          if (lvl != l) throw new UserBadDataError("Inconsistent level found for capsule " + c)
          if (continue) toProceed += (c → l)
      }
    }
    cache
  }

}

case class Mole(
  val root:         Capsule,
  val transitions:  Iterable[ITransition] = Iterable.empty,
  val dataChannels: Iterable[DataChannel] = Iterable.empty,
  val inputs:       PrototypeSet          = PrototypeSet.empty
) {

  lazy val slots = (Slot(root) :: transitions.map(_.end).toList).groupBy(_.capsule).mapValues(_.toSet).withDefault(c ⇒ Iterable.empty)
  lazy val capsules = slots.keys
  lazy val inputTransitions = transitions.groupBy(_.end).mapValues(_.toSet).withDefault(c ⇒ Iterable.empty)
  lazy val outputTransitions = transitions.groupBy(_.start).mapValues(_.toSet).withDefault(c ⇒ Iterable.empty)
  lazy val inputDataChannels = dataChannels.groupBy(_.end).mapValues(_.toSet).withDefault(c ⇒ Iterable.empty)
  lazy val outputDataChannels = dataChannels.groupBy(_.start).mapValues(_.toSet).withDefault(c ⇒ Iterable.empty)

  lazy val levels = Mole.levels(this)
  def level(c: Capsule) = levels(c)
}
