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

package org.openmole.core.workflow.validation

import org.openmole.core.context.{ Val, ValType }
import org.openmole.core.workflow.mole.{ Capsule, Hook, Source }
import org.openmole.core.workflow.transition.Slot
import org.openmole.core.workflow.validation.TypeUtil.InvalidType

object DataflowProblem {

  trait SlotDataflowProblem extends DataflowProblem {
    def slot: Slot
    def capsule = slot.capsule
  }

  sealed trait SlotType
  case object Input extends SlotType {
    override def toString = "input"
  }
  case object Output extends SlotType {
    override def toString = "output"
  }

  case class WrongType(
    slot:     Slot,
    expected: Val[_],
    provided: Val[_]
  ) extends SlotDataflowProblem {

    override def toString = "Wrong type received at " + slot + ", " + expected + " is expected but " + provided + " is provided."
  }

  case class MissingInput(
    slot: Slot,
    data: Val[_]
  ) extends SlotDataflowProblem {

    override def toString = "Input " + data + " is missing when reaching the " + slot + "."
  }

  case class DuplicatedName(
    capsule:   Capsule,
    name:      String,
    prototype: Iterable[Val[_]],
    slotType:  SlotType
  ) extends DataflowProblem {

    override def toString = name + " has been found several time with different types in " + slotType + " of capsule " + capsule + ": " + prototype.mkString(", ") + "."
  }

  case class IncoherentTypesBetweenSlots(
    capsule: Capsule,
    name:    String,
    types:   Iterable[ValType[_]]
  ) extends DataflowProblem {

    override def toString = name + " is present in multiple slot of capsule " + capsule + " but has different types: " + types.mkString(", ") + "."
  }

  case class IncoherentTypeAggregation(
    slot:   Slot,
    `type`: InvalidType
  ) extends SlotDataflowProblem {
    override def toString = s"Cannot aggregate type for slot ${slot}, the incoming data type are inconsistent (it may be because variables with the same name but not the same type reach the slot): ${`type`}."
  }

  sealed trait SourceProblem extends SlotDataflowProblem

  case class MissingSourceInput(
    slot:   Slot,
    source: Source,
    input:  Val[_]
  ) extends SourceProblem {

    override def toString = s"Input $input is missing for source $source at $slot"
  }

  case class WrongSourceType(
    slot:     Slot,
    source:   Source,
    expected: Val[_],
    provided: Val[_]
  ) extends SourceProblem {

    override def toString = s"Wrong type received for source $source at $slot, $expected is expected but $provided is provided."
  }

  sealed trait HookProblem extends DataflowProblem

  case class MissingHookInput(
    capsule: Capsule,
    hook:    Hook,
    input:   Val[_]
  ) extends HookProblem {

    override def toString = s"Input $input is missing for misc $hook"
  }
  case class WrongHookType(
    capsule: Capsule,
    hook:    Hook,
    input:   Val[_],
    found:   Val[_]
  ) extends HookProblem {

    override def toString = s"Input has incompatible type $found whereas $input was expected for hook $hook of capsule $capsule"
  }

  case class MissingMoleTaskImplicit(
    capsule:    Capsule,
    `implicit`: String
  ) extends DataflowProblem {

    override def toString = s"Implicit ${`implicit`} not found in input of $capsule"
  }

  case class MoleTaskDataFlowProblem(capsule: Capsule, problem: DataflowProblem) extends DataflowProblem {
    override def toString = s"Error in mole task $capsule: $problem"
  }

}

trait DataflowProblem extends Problem {
  def capsule: Capsule
}

