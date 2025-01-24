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
import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.workflow.transition._
import org.openmole.core.argument.Validate

import scala.collection._
import monocle.Focus

object Mole {

  def nextCapsules(mole: Mole)(from: MoleCapsule, lvl: Int) =
    nextTransitions(mole)(from, lvl).map { case (t, lvl) => t.end.capsule -> lvl }

  def nextTransitions(mole: Mole)(from: MoleCapsule, lvl: Int) =
    mole.outputTransitions(from).map {
      case t if Transition.isAggregation(t)    => t -> (lvl - 1)
      case t if Transition.isEndExploration(t) => t -> (lvl - 1)
      case t if Transition.isSlave(t)          => t -> lvl
      case t if Transition.isExploration(t)    => t -> (lvl + 1)
      case t                                   => t -> lvl
    }

  /**
   * Computes and checks the levels of capsules in a [[org.openmole.core.workflow.mole.Mole]].
   * The root level is 0, explorations increase the level whereas aggregation decrease it.
   *
   * @param mole
   * @return
   */
  def levels(mole: Mole) = {
    val cache = mutable.HashMap(mole.root -> 0)
    val toProceed = mutable.ListBuffer(mole.root -> 0)

    while (!toProceed.isEmpty) {
      val (capsule, level) = toProceed.remove(0)
      nextCapsules(mole)(capsule, level).foreach {
        case (c, l) =>
          val continue = !cache.contains(c)
          val lvl = cache.getOrElseUpdate(c, l)
          if (lvl != l) throw new UserBadDataError("Inconsistent level found for capsule " + c)
          if (continue) toProceed += (c -> l)
      }
    }
    cache
  }

}

/**
 * A Mole contains a [[org.openmole.core.workflow.mole.MoleCapsule]] and associates it to transitions, data channels and inputs.
 *
 * @param root
 * @param transitions
 * @param dataChannels
 * @param inputs
 */
case class Mole(
  root:         MoleCapsule,
  transitions:  Iterable[Transition]  = Iterable.empty,
  dataChannels: Iterable[DataChannel] = Iterable.empty,
  inputs:       PrototypeSet          = PrototypeSet.empty,
  validate:     Validate              = Validate.success
):

  lazy val slots = (TransitionSlot(root) :: transitions.map(_.end).toList).groupBy(_.capsule).map { case (k, v) => k -> v.toSet }.withDefault(c => Iterable.empty)
  lazy val capsules = slots.keys
  lazy val inputTransitions = transitions.groupBy(_.end).map { case (k, v) => k -> v.toSet }.withDefault(c => Iterable.empty)
  lazy val outputTransitions = transitions.groupBy(_.start).map { case (k, v) => k -> v.toSet }.withDefault(c => Iterable.empty)
  lazy val inputDataChannels = dataChannels.groupBy(_.end).map { case (k, v) => k -> v.toSet }.withDefault(c => Iterable.empty)
  lazy val outputDataChannels = dataChannels.groupBy(_.start).map { case (k, v) => k -> v.toSet }.withDefault(c => Iterable.empty)

  lazy val levels = Mole.levels(this)
  def level(c: MoleCapsule) =
    levels.get(c) match
      case Some(l) => l
      case None    => throw new InternalProcessingError(s"Capsule $c not found in $this")
