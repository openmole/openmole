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

import org.openmole.core.context._
import org.openmole.core.exception._
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.tools.DefaultSet
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.validation._
import org.openmole.tool.random._

object MoleCapsule {

  implicit def taskToCapsuleConverter(task: Task) = MoleCapsule(task)
  implicit def slotToCapsuleConverter(slot: TransitionSlot) = slot.capsule

  def apply(task: Task, strain: Boolean = false) = new MoleCapsule(task, strain)

  def isStrainer(c: MoleCapsule) = c.strainer

  def reachNoStrainer(mole: Mole)(slot: TransitionSlot, seen: Set[TransitionSlot] = Set.empty): Boolean = {
    if (slot.capsule == mole.root) true
    else if (seen.contains(slot)) false
    else {
      val capsules = mole.inputTransitions(slot).map { _.start } ++ mole.inputDataChannels(slot).map { _.start }
      val noStrainer =
        for {
          c ← capsules
          if isStrainer(c)
          s ← mole.slots(c)
        } yield reachNoStrainer(mole)(s, seen + slot)

      noStrainer.forall(_ == true)
    }
  }
}

/**
 * A capsule containing a task.
 *
 * @param _task task inside this capsule
 * @param strainer true if this capsule let pass all data through
 */
class MoleCapsule(_task: Task, val strainer: Boolean) {

  lazy val task =
    strainer match {
      case false ⇒ _task
      case true  ⇒ new StrainerTaskDecorator(_task)
    }

  /**
   * Get the inputs data taken by this capsule, generally it is empty if the capsule
   * is empty or the input of the task inside the capsule. It can be different
   * in some cases.
   *
   * @return the input of the capsule
   */
  def inputs(mole: Mole, sources: Sources, hooks: Hooks): PrototypeSet =
    strainerInputs(mole, sources, hooks) ++ capsuleInputs(mole, sources, hooks)

  /**
   * Get the outputs data taken by this capsule, generally it is empty if the capsule
   * is empty or the output of the task inside the capsule. It can be different
   * in some cases.
   *
   * @return the output of the capsule
   */
  def outputs(mole: Mole, sources: Sources, hooks: Hooks): PrototypeSet =
    strainerOutputs(mole, sources, hooks) + capsuleOutputs(mole, sources, hooks)

  def capsuleInputs(mole: Mole, sources: Sources, hooks: Hooks): PrototypeSet =
    task.inputs -- sources(this).flatMap(_.outputs) -- sources(this).flatMap(_.inputs) ++ sources(this).flatMap(_.inputs)

  def capsuleOutputs(mole: Mole, sources: Sources, hooks: Hooks): PrototypeSet =
    task.outputs -- hooks(this).flatMap(_.outputs) ++ hooks(this).flatMap(_.outputs)

  def strainerInputs(mole: Mole, sources: Sources, hooks: Hooks): PrototypeSet =
    if (strainer) {
      lazy val capsInputs = capsuleInputs(mole, sources, hooks)
      received(mole, sources, hooks).filterNot(d ⇒ capsInputs.contains(d.name))
    }
    else PrototypeSet.empty

  def strainerOutputs(mole: Mole, sources: Sources, hooks: Hooks): PrototypeSet =
    if (strainer) {
      lazy val capsOutputs = capsuleOutputs(mole, sources, hooks)
      received(mole, sources, hooks).filterNot(d ⇒ capsOutputs.contains(d.name))
    }
    else PrototypeSet.empty

  private def received(mole: Mole, sources: Sources, hooks: Hooks): PrototypeSet =
    if (this == mole.root) mole.inputs
    else {
      val slots = mole.slots(this)
      val noStrainer = slots.toSeq.filter(s ⇒ MoleCapsule.reachNoStrainer(mole)(s))

      val bySlot =
        for {
          slot ← noStrainer
          received = TypeUtil.validTypes(mole, sources, hooks)(slot)
        } yield received.map(_.toPrototype)

      val allNames = bySlot.toSeq.flatMap(_.map(_.name)).distinct
      val byName = bySlot.map(_.toSeq.groupBy(_.name).withDefaultValue(Seq.empty))

      def haveAllTheSameType(ps: Seq[Val[_]]) = ps.map(_.`type`).distinct.size == 1
      def inAllSlots(ps: Seq[Val[_]]) = ps.size == noStrainer.size

      val prototypes =
        for {
          name ← allNames
          inSlots = byName.map(_(name).toSeq).toSeq
          if inSlots.forall(haveAllTheSameType)
          oneBySlot = inSlots.map(_.head)
          if inAllSlots(oneBySlot) && haveAllTheSameType(oneBySlot)
        } yield oneBySlot.head

      prototypes
    }

  override def toString = s"capsule@$hashCode:$task"
}

class StrainerTaskDecorator(val task: Task) extends Task {
  override def info = task.info
  override def config = task.config
  override def perform(context: Context, executionContext: TaskExecutionContext): Context = context + task.perform(context, executionContext)
  override def process(executionContext: TaskExecutionContext): FromContext[Context] = throw new InternalProcessingError("This method should never be called")
}

object StrainerCapsule {
  def apply(task: Task) = MoleCapsule(task, strain = true)
}

object MasterCapsule {
  def apply(task: Task, persist: Seq[Val[_]], strain: Boolean) = new MasterCapsule(task, persist.map(_.name), strain)
  def apply(t: Task, persist: Val[_]*): MasterCapsule = apply(t, persist, false)
}

class MasterCapsule(task: Task, val persist: Seq[String] = Seq.empty, strainer: Boolean) extends MoleCapsule(task, strainer) {
  def toPersist(context: Context): Context =
    persist.map { n ⇒ context.variable(n).get }

}