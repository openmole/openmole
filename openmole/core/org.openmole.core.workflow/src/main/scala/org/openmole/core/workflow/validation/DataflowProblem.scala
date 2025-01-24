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
import org.openmole.core.workflow.hook.Hook
import org.openmole.core.workflow.mole.{ MoleCapsule, Source }
import org.openmole.core.workflow.transition.TransitionSlot
import org.openmole.core.workflow.validation.TypeUtil.InvalidType

object DataflowProblem:

  trait SlotDataflowProblem extends DataflowProblem:
    def slot: TransitionSlot
    def capsule = slot.capsule

  sealed trait SlotType
  case object Input extends SlotType:
    override def toString = "input"

  case object Output extends SlotType:
    override def toString = "output"

  case class WrongType(
    slot:     TransitionSlot,
    expected: Val[?],
    provided: Val[?]
  ) extends SlotDataflowProblem:
    override def toString = "Wrong type received at " + slot + ", " + expected.quotedString + " is expected but " + provided.quotedString + " is provided."

  case class MissingInput(
    slot:    TransitionSlot,
    data:    Val[?],
    reaches: Seq[Val[?]]
  ) extends SlotDataflowProblem:
    override def toString = s"Input ${data.quotedString} is missing when reaching the " + slot + s""", available inputs are: ${reaches.map(_.quotedString).mkString(",")}."""


  case class DuplicatedName(
    capsule:   MoleCapsule,
    name:      String,
    prototype: Iterable[Val[?]],
    slotType:  SlotType
  ) extends DataflowProblem:
    override def toString = name + " has been found several time with different types in " + slotType + " of capsule " + capsule + ": " + prototype.map(_.quotedString).mkString(", ") + "."

  case class IncoherentTypesBetweenSlots(
    capsule: MoleCapsule,
    name:    String,
    types:   Iterable[ValType[?]]
  ) extends DataflowProblem:
    override def toString = name + " is present in multiple slot of capsule " + capsule + " but has different types: " + types.mkString(", ") + "."

  case class IncoherentTypeAggregation(
    slot:   TransitionSlot,
    `type`: InvalidType
  ) extends SlotDataflowProblem:
    override def toString = s"Cannot aggregate type for slot ${slot}, the incoming data type are inconsistent (it may be because variables with the same name but not the same type reach the slot): ${`type`}."

  sealed trait SourceProblem extends SlotDataflowProblem

  case class MissingSourceInput(
    slot:   TransitionSlot,
    source: Source,
    input:  Val[?]
  ) extends SourceProblem:
    override def toString = s"Input ${input.quotedString} is missing for source $source at $slot"

  case class WrongSourceType(
    slot:     TransitionSlot,
    source:   Source,
    expected: Val[?],
    provided: Val[?]
  ) extends SourceProblem:
    override def toString = s"Wrong type received for source $source at $slot, ${expected.quotedString} is expected but ${provided.quotedString} is provided."

  sealed trait HookProblem extends DataflowProblem

  case class MissingHookInput(
    capsule: MoleCapsule,
    hook:    Hook,
    input:   Val[?]
  ) extends HookProblem :
    override def toString = s"Input ${input.quotedString} is missing for hook $hook"

  case class WrongHookType(
    capsule: MoleCapsule,
    hook:    Hook,
    input:   Val[?],
    found:   Val[?]
  ) extends HookProblem:
    override def toString = s"Input has incompatible type ${found.quotedString} whereas ${input.quotedString} was expected for hook $hook of capsule $capsule"

  case class MissingMoleTaskImplicit(
    capsule:    MoleCapsule,
    `implicit`: String
  ) extends DataflowProblem:
    override def toString = s"Implicit ${`implicit`} not found in input of $capsule"

  case class MoleTaskDataFlowProblem(capsule: MoleCapsule, problem: DataflowProblem) extends DataflowProblem:
    override def toString = s"Error in mole task $capsule: $problem"


trait DataflowProblem extends Problem:
  def capsule: MoleCapsule

