/**
 * Created by Romain Reuillon on 22/04/16.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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
 *
 */
package org.openmole.core.workflow.validation

import org.openmole.core.argument.Validate
import org.openmole.core.exception.*
import org.openmole.core.setter.*
import org.openmole.core.tools.io.Prettifier
import org.openmole.core.workflow.hook.*
import org.openmole.core.workflow.mole.*
import org.openmole.core.workflow.task.*
import org.openmole.core.workflow.transition.*
import org.openmole.core.workflow.validation.TypeUtil.*
import org.openmole.core.context.*

object ValidateTask:
  def validate(task: Task): Validate =
    task match
      case t: ValidateTask => t.validate
      case _ => Validate.success

trait ValidateTask:
  def validate: Validate

trait ValidateSource:
  def validate: Validate

trait ValidateHook:
  def validate: Validate

trait ValidateTransition:
  def validate: Validate

object ValidationProblem:

  private def exceptionToString(e: Throwable) =
    e match
      case e: UserBadDataError => Prettifier.insertMargin(e.message, 2)
      case e => Prettifier.stackStringWithMargin(e, 2)

//    def stripFirstExceptionName(s: String) =
//      val lines = s.split("\n")
//      val first =
//        if
//          lines.head.contains(classOf[UserBadDataError].getCanonicalName) ||
//            lines.head.contains(classOf[InternalProcessingError].getCanonicalName)
//        then
//          val head = lines.head.takeWhile(Set(' ', '|').contains)
//          s"${head}- " + lines.head.dropWhile(_ != ':').drop(2)
//        else lines.head
//
//      (Seq(first) ++ lines.drop(1)).mkString("\n")
//    stripFirstExceptionName(Prettifier.stackStringWithMargin(e))

  def errorsString(size: Int) =
    size match
      case 1 => "one error"
      case 2 => "two errors"
      case 3 => "three errors"
      case n => s"$n errors"


  case class TaskValidationProblem(task: Task, errors: Seq[Throwable]) extends ValidationProblem:
    override def toString = s"Validation of ${Problem.definedElement(task)}:\n" + errors.map(exceptionToString).mkString("\n")

  case class SourceValidationProblem(source: Source, errors: Seq[Throwable]) extends ValidationProblem:
    override def toString = s"Validation of ${Problem.definedElement(source)}:\n" + errors.map(exceptionToString).mkString("\n")

  case class HookValidationProblem(hook: Hook, errors: Seq[Throwable]) extends ValidationProblem:
    override def toString = s"Validation of ${Problem.definedElement(hook)}:\n" + errors.map(exceptionToString).mkString("\n")

  case class TransitionValidationProblem(transition: Transition, errors: Seq[Throwable]) extends ValidationProblem:
    override def toString = s"Validation of transition $transition:\n" + errors.map(exceptionToString).mkString("\n")

  case class MoleValidationProblem(mole: Mole, errors: Seq[Throwable]) extends ValidationProblem:
    override def toString = s"Validation of mole:\n" + errors.map(exceptionToString).mkString("\n")


sealed trait ValidationProblem extends Problem


object DataflowProblem:

  trait SlotDataflowProblem extends DataflowProblem:
    def slot: TransitionSlot
    def capsule = slot.capsule

  enum SlotType:
    case Input, Output

  case class WrongType(
    slot:     TransitionSlot,
    expected: Val[?],
    provided: Val[?]
  ) extends SlotDataflowProblem:
    override def toString = s"Wrong type received ${expected.quotedString} is expected but ${provided.quotedString} is provided, to ${Problem.definedElement(slot)}."

  case class MissingInput(
    slot:    TransitionSlot,
    data:    Val[?],
    reaches: Seq[Val[?]]
  ) extends SlotDataflowProblem:
    override def toString = s"Input ${data.quotedString} is missing, available inputs are ${reaches.map(_.quotedString).mkString(",")}, for the ${Problem.definedElement(slot)}."


  case class DuplicatedName(
    capsule:   MoleCapsule,
    name:      String,
    prototype: Iterable[Val[?]],
    slotType:  SlotType
  ) extends DataflowProblem:
    override def toString =
      val part =
        slotType match
          case SlotType.Input => "input"
          case SlotType.Output => "output"

      s"$name has been found several time with different types \"${prototype.map(_.quotedString).mkString(", ")}\" in $part of capsule ${Problem.definedElement(capsule)}: ."

  case class IncoherentTypesBetweenSlots(
    capsule: MoleCapsule,
    name:    String,
    types:   Iterable[ValType[?]]
  ) extends DataflowProblem:
    override def toString = s"$name is present in multiple slot of capsule ${Problem.definedElement(capsule)} but has different types: ${types.mkString(", ")}."

  case class IncoherentTypeAggregation(
    slot:   TransitionSlot,
    `type`: InvalidType
  ) extends SlotDataflowProblem:
    override def toString = s"Cannot aggregate type for slot ${Problem.definedElement(slot)}, the incoming data type are inconsistent (it may be because variables with the same name but not the same type reach the slot): ${`type`}."

  sealed trait SourceProblem extends SlotDataflowProblem

  case class MissingSourceInput(
    slot:   TransitionSlot,
    source: Source,
    input:  Val[?]
  ) extends SourceProblem:
    override def toString = s"Input ${input.quotedString} is missing for ${Problem.definedElement(source)} to ${Problem.definedElement(slot)}"

  case class WrongSourceType(
    slot:     TransitionSlot,
    source:   Source,
    expected: Val[?],
    provided: Val[?]
  ) extends SourceProblem:
    override def toString = s"Wrong type received, ${expected.quotedString} is expected but ${provided.quotedString} is provided, for ${Problem.definedElement(source)} to ${Problem.definedElement(slot)}."

  sealed trait HookProblem extends DataflowProblem

  case class MissingHookInput(
    capsule: MoleCapsule,
    hook:    Hook,
    input:   Val[?]
  ) extends HookProblem :
    override def toString = s"Input ${input.quotedString} is missing for ${Problem.definedElement(hook)}."

  case class WrongHookType(
    capsule: MoleCapsule,
    hook:    Hook,
    input:   Val[?],
    found:   Val[?]
  ) extends HookProblem:
    override def toString = s"Input has incompatible type ${found.quotedString} whereas ${input.quotedString} was expected for ${Problem.definedElement(hook)}."

  case class MissingMoleTaskImplicit(
    capsule:    MoleCapsule,
    `implicit`: String
  ) extends DataflowProblem:
    override def toString = s"Implicit ${`implicit`} not found in input of ${Problem.definedElement(capsule)}"

  case class MoleTaskDataFlowProblem(capsule: MoleCapsule, problem: DataflowProblem) extends DataflowProblem:
    override def toString = s"MoleTaskDataFlowProblem: Error in ${Problem.definedElement(capsule)}: $problem"


trait DataflowProblem extends Problem:
  def capsule: MoleCapsule

/**
 * Problems with the validation of Puzzle's topology
 */
object TopologyProblem:

  case class DuplicatedTransition(transitions: Iterable[Transition]) extends TopologyProblem:
    override def toString = "DuplicatedTransition: from " + transitions.head.start + " to " + transitions.head.end.capsule + " has been found " + transitions.size + " times."


  case class LevelProblem(
    capsule: MoleCapsule,
    paths:   List[(List[MoleCapsule], Int)]
  ) extends TopologyProblem:
    override def toString = "LevelProblem: " + capsule + ", " + paths.map { case (p, l) => "Following the path (" + p.mkString(", ") + " has level " + l + ")" }.mkString(", ")


  case class NegativeLevelProblem(
    capsule: MoleCapsule,
    path:    List[MoleCapsule],
    level:   Int
  ) extends TopologyProblem:
    override def toString = s"LevelProblem: ${Problem.definedElement(capsule)}  has a negative level $level following the path ${path.mkString(", ")}"

  case class DataChannelNegativeLevelProblem(dataChannel: DataChannel) extends TopologyProblem:
    override def toString = s"DataChannelNegativeLevelProblem: $dataChannel, links a capsule of upper level to lower level, this is not supported, use aggregation transitions."

  case class NoTransitionToCapsuleProblem(capsule: MoleCapsule, dataChannel: DataChannel) extends TopologyProblem:
    override def toString = s"NoTransitionToCapsuleProblem: ${Problem.definedElement(capsule)} is linked with $dataChannel but not with any transition"

  case class UnreachableCapsuleProblem(capsule: MoleCapsule) extends TopologyProblem:
    override def toString = s"UnreachableCapsuleProblem: ${Problem.definedElement(capsule)} is not linked to the workflow by any transition"

  case class MoleTaskLastCapsuleProblem(capsule: MoleCapsule, moleTask: MoleTask, level: Int) extends TopologyProblem:
    override def toString = s"MoleTaskLastCapsuleProblem: in ${Problem.definedElement(capsule)} the last capsule ${moleTask.last} has been found at level $level, it should be at level 0"

trait TopologyProblem extends Problem

object Problem:
  def definedElement(t: Task | Source | Hook | MoleCapsule | TransitionSlot): String =
    t match
      case t: Task => s"task ${t.simpleName}${defined(t.info.definitionScope)}"
      case s: Source => s"source $s${defined(s.info.definitionScope)}"
      case h: Hook => s"hook $h${defined(h.info.definitionScope)}"
      case c: MoleCapsule => s"${definedElement(c._task)} (capsule id $c)"
      case s: TransitionSlot => s"${definedElement(s.capsule._task)} (slot id $s)"

  private def defined(scope: DefinitionScope) =
    (scope.line, scope.imported) match
      case (Some(l), _) => s", defined on line $l"
      case (_, Some(i)) => s", imported from file ${i.importedFrom.getName} by instruction \"import ${i.`import`}\""
      case _ => ""


sealed trait Problem
