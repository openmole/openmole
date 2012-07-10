/*
 * Copyright (C) 2012 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

package org.openmole.core.implementation.validation

import org.openmole.core.model.data.IData
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.transition.ISlot

object DataflowProblem {

  sealed trait SlotType
  case object Input extends SlotType
  case object Output extends SlotType

  case class WrongType(
      val slot: ISlot,
      val data: IData[_],
      val provided: IPrototype[_]) extends DataflowProblem {

    def capsule: ICapsule = slot.capsule

    override def toString = "Wrong type from " + slot + ", data " + data.prototype + " is expected but " + provided + " is provided."
  }

  case class MissingInput(
      val slot: ISlot,
      val data: IData[_]) extends DataflowProblem {

    def capsule: ICapsule = slot.capsule

    override def toString = "Input " + data + " is missing when reaching the " + slot + "."
  }

  case class DuplicatedName(
      val capsule: ICapsule,
      val name: String,
      val data: Iterable[IData[_]],
      val slotType: SlotType) extends DataflowProblem {
    override def toString = name + " has been found several time in capsule in " + slotType + " of capsule " + capsule + ": " + data.mkString(", ") + "."
  }

}

trait DataflowProblem extends Problem {
  def capsule: ICapsule
}