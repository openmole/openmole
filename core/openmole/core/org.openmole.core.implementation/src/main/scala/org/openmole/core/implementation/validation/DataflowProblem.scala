/*
 * Copyright (C) 2012 Romain Reuillon
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

import org.openmole.core.model.data.Data
import org.openmole.core.model.data.Prototype
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.transition.Slot

object DataflowProblem {

  sealed trait SlotType
  case object Input extends SlotType
  case object Output extends SlotType

  case class WrongType(
      val slot: Slot,
      val data: Data[_],
      val provided: Prototype[_]) extends DataflowProblem {

    def capsule: ICapsule = slot.capsule

    override def toString = "Wrong type received at " + slot + ", data " + data.prototype + " is expected but " + provided + " is provided."
  }

  case class MissingInput(
      val slot: Slot,
      val data: Data[_]) extends DataflowProblem {

    def capsule: ICapsule = slot.capsule

    override def toString = "Input " + data + " is missing when reaching the " + slot + "."
  }

  case class OptionalOutput(
      val slot: Slot,
      val data: Data[_]) extends DataflowProblem {

    def capsule: ICapsule = slot.capsule

    override def toString = "Input " + data + " is provided by an optional output when reaching the " + slot + " and no default value (parameter) is provided."
  }

  case class DuplicatedName(
      val capsule: ICapsule,
      val name: String,
      val data: Iterable[Data[_]],
      val slotType: SlotType) extends DataflowProblem {
    override def toString = name + " has been found several time in capsule in " + slotType + " of capsule " + capsule + ": " + data.mkString(", ") + "."
  }

  sealed trait HookProblem extends DataflowProblem

  case class MissingHookInput(
      val capsule: ICapsule,
      val input: Data[_]) extends Problem {
    override def toString = "Input is missing " + input
  }
  case class WrongHookType(
      val capsule: ICapsule,
      val input: Data[_],
      val found: Data[_]) extends Problem {
    override def toString = "Input has incompatible type " + found + " expected " + input
  }
  case class MissingMoleTaskImplicit(
      val capsule: ICapsule,
      val `implicit`: String) extends Problem {
    override def toString = s"Implicit ${`implicit`} not found in input of $capsule"
  }


}

trait DataflowProblem extends Problem {
  def capsule: ICapsule
}