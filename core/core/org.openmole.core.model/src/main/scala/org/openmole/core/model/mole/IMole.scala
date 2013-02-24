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
import collection._

trait IMole {
  def root: ICapsule
  def transitions: Iterable[ITransition]
  def dataChannels: Iterable[IDataChannel]

  lazy val slots = (Slot(root) :: transitions.map(_.end).toList).groupBy(_.capsule).mapValues(_.toSet).withDefault(c ⇒ Iterable.empty)
  lazy val capsules = slots.keys
  lazy val inputTransitions = transitions.groupBy(_.end).mapValues(_.toSet).withDefault(c ⇒ Iterable.empty)
  lazy val outputTransitions = transitions.groupBy(_.start).mapValues(_.toSet).withDefault(c ⇒ Iterable.empty)
  lazy val inputDataChannels = dataChannels.groupBy(_.end).mapValues(_.toSet).withDefault(c ⇒ Iterable.empty)
  lazy val outputDataChannels = dataChannels.groupBy(_.start).mapValues(_.toSet).withDefault(c ⇒ Iterable.empty)

  def level(c: ICapsule): Int
}
