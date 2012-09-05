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

package org.openmole.core.model.mole

import org.openmole.core.model.transition._
import org.openmole.core.model.data._
import org.openmole.core.model.task._

import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer

object IMole {

  def slots(mole: IMole): Seq[Slot] = {
    val visited = new HashSet[ICapsule]
    val list = new ListBuffer[Slot]
    val toExplore = new ListBuffer[ICapsule]

    toExplore += mole.root
    list += Slot(mole.root)

    while (!(toExplore.isEmpty)) {
      val current = toExplore.remove(0)

      if (!visited.contains(current)) {
        for (transition ← mole.outputTransitions(current)) {
          toExplore += transition.end.capsule
          list += transition.end
        }
        visited += current
      }
    }
    list
  }

}

trait IMole {
  def root: ICapsule
  def transitions: Iterable[ITransition]
  def dataChannels: Iterable[IDataChannel]
  def implicits: Context

  lazy val slots = IMole.slots(this).groupBy(_.capsule).mapValues(_.toSet).withDefault(c ⇒ Iterable.empty)
  lazy val capsules = slots.unzip._1
  lazy val inputTransitions = transitions.groupBy(_.end).mapValues(_.toSet).withDefault(c ⇒ Iterable.empty)
  lazy val outputTransitions = transitions.groupBy(_.start).mapValues(_.toSet).withDefault(c ⇒ Iterable.empty)
  lazy val inputDataChannels = dataChannels.groupBy(_.end).mapValues(_.toSet).withDefault(c ⇒ Iterable.empty)
  lazy val outputDataChannels = dataChannels.groupBy(_.start).mapValues(_.toSet).withDefault(c ⇒ Iterable.empty)

  def level(c: ICapsule): Int
}
