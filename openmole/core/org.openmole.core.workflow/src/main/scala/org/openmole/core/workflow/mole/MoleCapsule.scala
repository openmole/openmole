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
import org.openmole.core.argument.{DefaultSet, FromContext}
import org.openmole.core.workflow.job.RuntimeTask
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.validation._
import org.openmole.tool.random._
import monocle.Focus

object MoleCapsule:

  case class Master(persist: Seq[String])

  def apply(task: Task, strain: Boolean = false, funnel: Boolean = false, master: Option[Master] = None) =
    new MoleCapsule(task, strain = strain, funnel = funnel || MoleTask.isMoleTask(task), master = master)

  def isStrainer(c: MoleCapsule) = c.strain

  /* Test wether there is a path from this slot reaching the root of the mole without looping to the capsule it is bounded to */
  def reachRootWithNoLoop(mole: Mole)(slot: TransitionSlot): Boolean =
    def previousCapsules(s: TransitionSlot) = mole.inputTransitions(s).map { _.start }
    def loopToCapsule(s: TransitionSlot) = previousCapsules(s).exists(_ == slot.capsule)

    var reachRoot = false

    val seen = collection.mutable.Set[MoleCapsule]()
    val toProceed = collection.mutable.Stack[TransitionSlot]()

    toProceed.pushAll(previousCapsules(slot).flatMap(mole.slots))

    while !reachRoot && !toProceed.isEmpty
    do
      val s = toProceed.pop()

      if !loopToCapsule(s)
      then
        if s.capsule == mole.root
        then reachRoot = true
        else
          val capsules = previousCapsules(s)

          for
            c <- capsules
            if !seen.contains(c)
            s <- mole.slots(c)
          do toProceed.push(s)

          seen ++= capsules

    reachRoot

  def received(capsule: MoleCapsule, mole: Mole, sources: Sources, hooks: Hooks): PrototypeSet =
    if capsule == mole.root
    then mole.inputs
    else
      val slots = mole.slots(capsule)
      val noStrainer = slots.toSeq.filter(s => MoleCapsule.reachRootWithNoLoop(mole)(s))

      val bySlot =
        for
          slot <- noStrainer
          received = TypeUtil.validTypes(mole, sources, hooks)(slot)
        yield received.map(_.toVal)

      val allNames = bySlot.toSeq.flatMap(_.map(_.name)).distinct
      val byName = bySlot.map(_.toSeq.groupBy(_.name).withDefaultValue(Seq.empty))

      def haveAllTheSameType(ps: Seq[Val[?]]) = ps.map(_.`type`).distinct.size == 1
      def inAllSlots(ps: Seq[Val[?]]) = ps.size == noStrainer.size

      val prototypes =
        for
          name <- allNames
          inSlots = byName.map(_(name).toSeq).toSeq
          if inSlots.forall(haveAllTheSameType)
          oneBySlot = inSlots.map(_.head)
          if inAllSlots(oneBySlot) && haveAllTheSameType(oneBySlot)
        yield oneBySlot.head

      prototypes


  def completeMoleTask(capsule: MoleCapsule, mole: Mole, sources: Sources, hooks: Hooks) =
    capsule._task match
      case task: MoleTask => Focus[MoleTask](_.mole.inputs) modify (_ ++ capsule.inputs(mole, sources, hooks)) apply task
      case task => task


//  def runtimeTask(capsule: MoleCapsule, mole: Mole, sources: Sources, hooks: Hooks) =
//    val withInputs = capsule.task(mole, sources, hooks)
//    RuntimeTask(withInputs, capsule.strain)


/**
 * A capsule containing a task.
 *
 * @param _task task inside this capsule
 * @param strain true if this capsule let pass all data through
 */
class MoleCapsule(val _task: Task, val strain: Boolean, val funnel: Boolean, val master: Option[MoleCapsule.Master]) {

  def task(mole: Mole, sources: Sources, hooks: Hooks): Task = MoleCapsule.completeMoleTask(this, mole, sources, hooks)

  /**
   * Get the inputs data taken by this capsule, generally it is empty if the capsule
   * is empty or the input of the task inside the capsule. It can be different
   * in some cases.
   *
   * @return the input of the capsule
   */
  def inputs(mole: Mole, sources: Sources, hooks: Hooks): PrototypeSet = {
    if (strain || funnel) receivedInputs(mole, sources, hooks) ++ capsuleInputs(mole, sources, hooks)
    else capsuleInputs(mole, sources, hooks)
  }

  /**
   * Get the outputs data taken by this capsule, generally it is empty if the capsule
   * is empty or the output of the task inside the capsule. It can be different
   * in some cases.
   *
   * @return the output of the capsule
   */
  def outputs(mole: Mole, sources: Sources, hooks: Hooks): PrototypeSet =
    if (strain) strainedOutputs(mole, sources, hooks) ++ capsuleOutputs(mole, sources, hooks)
    else capsuleOutputs(mole, sources, hooks)

  def capsuleInputs(mole: Mole, sources: Sources, hooks: Hooks): PrototypeSet =
    Task.inputs(_task) -- sources(this).flatMap(_.outputs) -- sources(this).flatMap(_.inputs) ++ sources(this).flatMap(_.inputs)

  def capsuleOutputs(mole: Mole, sources: Sources, hooks: Hooks): PrototypeSet =
    Task.outputs(_task) -- hooks(this).flatMap(_.outputs) ++ hooks(this).flatMap(_.outputs)

  private def receivedInputs(mole: Mole, sources: Sources, hooks: Hooks) = {
    lazy val capsInputs = capsuleInputs(mole, sources, hooks)
    MoleCapsule.received(this, mole, sources, hooks).filterNot(d => capsInputs.contains(d.name))
  }

  private def strainedOutputs(mole: Mole, sources: Sources, hooks: Hooks) = {
    lazy val capsOutputs = capsuleOutputs(mole, sources, hooks)
    MoleCapsule.received(this, mole, sources, hooks).filterNot(d => capsOutputs.contains(d.name))
  }

  override def toString =
    (if (!strain) "capsule" else "strainerCapsule") + s"@$hashCode:${_task}"

}

object StrainerCapsule {
  def apply(task: Task) = MoleCapsule(task, strain = true)
}

object MasterCapsule {
  def apply(task: Task, persist: Seq[Val[?]], strain: Boolean) = MoleCapsule(task, strain = strain, master = Some(MoleCapsule.Master(persist.map(_.name))))
  def apply(t: Task, persist: Val[?]*): MoleCapsule = apply(t, persist, false)
  def toPersist(master: MoleCapsule.Master, context: Context): Context = master.persist.map { n => context.variables.getOrElse(n, throw new UserBadDataError(s"Variable $n has not been found in the context")) }
}
