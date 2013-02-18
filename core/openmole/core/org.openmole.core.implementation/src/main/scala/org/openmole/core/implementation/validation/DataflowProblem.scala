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

import org.openmole.core.model.data._
import org.openmole.core.model.mole.{ISource, Hook, ICapsule}
import org.openmole.core.model.transition.Slot

object DataflowProblem {

  sealed trait SlotType
  case object Input extends SlotType
  case object Output extends SlotType

  case class WrongType(
      slot: Slot,
      data: Data[_],
      provided: Prototype[_]) extends DataflowProblem {

    def capsule: ICapsule = slot.capsule

    override def toString = "Wrong type received at " + slot + ", data " + data.prototype + " is expected but " + provided + " is provided."
  }

  case class MissingInput(
      slot: Slot,
      data: Data[_]) extends DataflowProblem {

    def capsule: ICapsule = slot.capsule

    override def toString = "Input " + data + " is missing when reaching the " + slot + "."
  }

  case class OptionalOutput(
      slot: Slot,
      data: Data[_]) extends DataflowProblem {

    def capsule: ICapsule = slot.capsule

    override def toString = "Input " + data + " is provided by an optional output when reaching the " + slot + " and no default value (parameter) is provided."
  }

  case class DuplicatedName(
      capsule: ICapsule,
      name: String,
      data: Iterable[Data[_]],
      slotType: SlotType) extends DataflowProblem {
    override def toString = name + " has been found several time in capsule in " + slotType + " of capsule " + capsule + ": " + data.mkString(", ") + "."
  }

  sealed trait HookProblem extends DataflowProblem

  case class MissingSourceInput(
      slot: Slot,
      source: ISource,
      input: Data[_]) extends Problem {
    override def toString = s"Input $input is missing for source $source at $slot"
  }

   case class WrongSourceType(
      slot: Slot,
      source: ISource,
      data: Data[_],
      provided: Prototype[_]) extends Problem {

     override def toString = s"Wrong type received for source $source at $slot, data ${data.prototype} is expected but $provided is provided."
  }

   case class OptionalSourceOutput(
      slot: Slot,
      source: ISource,
      data: Data[_]) extends DataflowProblem {

    def capsule: ICapsule = slot.capsule

    override def toString = "Input " + data + " is provided by an optional output for source $source when reaching the " + slot + " and no default value (parameter) is provided."
  }


  case class MissingHookInput(
      capsule: ICapsule,
      hook: Hook,
      input: Data[_]) extends Problem {
    override def toString = s"Input $input is missing for hook $hook"
  }
  case class WrongHookType(
      capsule: ICapsule,
      hook: Hook,
      input: Data[_],
      found: Data[_]) extends Problem {
    override def toString = s"Input has incompatible type $found whereas $input was expected"
  }
  case class MissingMoleTaskImplicit(
      capsule: ICapsule,
      `implicit`: String) extends Problem {
    override def toString = s"Implicit ${`implicit`} not found in input of $capsule"
  }


}

trait DataflowProblem extends Problem {
  def capsule: ICapsule
}