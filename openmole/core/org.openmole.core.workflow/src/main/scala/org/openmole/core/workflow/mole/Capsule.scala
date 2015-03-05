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

import org.openmole.core.workflow.data._
import org.openmole.core.workflow.job._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.validation.TypeUtil

object Capsule {

  implicit def taskToCapsuleConverter(task: Task) = Capsule(task)
  implicit def slotToCapsuleConverter(slot: Slot) = slot.capsule

  def apply(task: Task, strainer: Boolean = false) = new Capsule(task, strainer)

  def isStrainer(c: Capsule) = c.strainer

  def reachNoStrainer(mole: Mole)(slot: Slot, seen: Set[Slot] = Set.empty): Boolean = {
    if (slot.capsule == mole.root) true
    else if (seen.contains(slot)) false
    else {
      val capsules = mole.inputTransitions(slot).map { _.start } ++ mole.inputDataChannels(slot).map { _.start }
      val noStrainer =
        for (c ← capsules; if (isStrainer(c)); s ← mole.slots(c)) yield reachNoStrainer(mole)(s, seen + slot)
      noStrainer.foldLeft(true)(_ & _)
    }
  }
}

/**
 * A capsule containing a task.
 *
 * @param _task task inside this capsule
 * @param strainer true if this capsule let pass all data through
 */
class Capsule(_task: Task, val strainer: Boolean) {

  def task =
    strainer match {
      case false ⇒ _task
      case true  ⇒ new StrainerTaskDecorator(_task)
    }

  /*
   * Get the inputs data taken by this capsule, generally it is empty if the capsule
   * is empty or the input of the task inside the capsule. It can be different
   * in some cases.
   * 
   * @return the input of the capsule
   */
  def inputs(mole: Mole, sources: Sources, hooks: Hooks): DataSet =
    strainerInputs(mole, sources, hooks) ++ capsuleInputs(mole, sources, hooks)

  /*
   * Get the outputs data taken by this capsule, generally it is empty if the capsule
   * is empty or the output of the task inside the capsule. It can be different
   * in some cases.
   *
   * @return the output of the capsule
   */
  def outputs(mole: Mole, sources: Sources, hooks: Hooks): DataSet =
    strainerOutputs(mole, sources, hooks) + capsuleOutputs(mole, sources, hooks)

  def capsuleInputs(mole: Mole, sources: Sources, hooks: Hooks): DataSet =
    task.inputs -- sources(this).flatMap(_.outputs) -- sources(this).flatMap(_.inputs) ++ sources(this).flatMap(_.inputs)

  def capsuleOutputs(mole: Mole, sources: Sources, hooks: Hooks): DataSet =
    task.outputs -- hooks(this).flatMap(_.outputs) ++ hooks(this).flatMap(_.outputs)

  def strainerInputs(mole: Mole, sources: Sources, hooks: Hooks): DataSet =
    if (strainer) received(mole, sources, hooks).filterNot(d ⇒ capsuleInputs(mole, sources, hooks).contains(d.prototype.name))
    else DataSet.empty

  def strainerOutputs(mole: Mole, sources: Sources, hooks: Hooks): DataSet =
    if (strainer) received(mole, sources, hooks).filterNot(d ⇒ capsuleOutputs(mole, sources, hooks).contains(d.prototype.name))
    else DataSet.empty

  def received(mole: Mole, sources: Sources, hooks: Hooks) =
    if (this == mole.root) Iterable.empty
    else {
      val slots = mole.slots(this)
      val noStrainer = slots.filter(s ⇒ Capsule.reachNoStrainer(mole)(s))
      TypeUtil.intersect(noStrainer.map { TypeUtil.receivedTypes(mole, sources, hooks) }).map(Data(_))
    }

  override def toString = task.toString
}

class StrainerTaskDecorator(val task: Task) extends Task {
  override def inputs = task.inputs
  override def outputs = task.outputs

  override def perform(context: Context) = process(context)
  override def process(context: Context) = context + task.perform(context)
  override def defaults = task.defaults

  override def name = task.name
}

object StrainerCapsule {
  def apply(task: Task) = Capsule(task, strainer = true)
}