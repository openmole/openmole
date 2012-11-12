/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.core.implementation.mole

import org.openmole.core.model.data._
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.validation._
import org.openmole.core.model.mole._
import org.openmole.core.model.task._
import org.openmole.core.model.transition._

object StrainerCapsule {

  def apply(task: ITask) = new StrainerCapsule(task)

  class StrainerTaskDecorator(val task: ITask) extends Task {
    override def name = task.name
    override def inputs = task.inputs
    override def outputs = task.outputs
    override def plugins = task.plugins
    override def perform(context: Context) = process(context)
    override def process(context: Context) = context + task.perform(context)
    override def parameters = task.parameters
  }

  def isStrainer(c: ICapsule) =
    c match {
      case _: StrainerCapsule ⇒ true
      case _ ⇒ false
    }

  /*def hasNoStrainer(mole: IMole)(slot: Slot) =    
    mole.inputTransitions(slot).forall(t => !isStrainer(t.start)) && 
    mole.inputDataChannels(slot).forall(dc => !isStrainer(dc.start))*/

  def reachNoStrainer(mole: IMole)(slot: Slot, seen: Set[Slot] = Set.empty): Boolean = {
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

import StrainerCapsule._

class StrainerCapsule(task: ITask) extends Capsule(new StrainerCapsule.StrainerTaskDecorator(task)) {

  def received(mole: IMole) =
    if (this == mole.root) Iterable.empty
    else {
      val slots = mole.slots(this)
      val noStrainer = slots.filter(s ⇒ reachNoStrainer(mole)(s))
      TypeUtil.intersect(noStrainer.map { TypeUtil.receivedTypes(mole) }).map(Data(_))
    }

  override def inputs(mole: IMole) =
    received(mole).filterNot(d ⇒ super.inputs(mole).contains(d.prototype.name)) ++
      super.inputs(mole)

  override def outputs(mole: IMole) =
    received(mole).filterNot(d ⇒ super.outputs(mole).contains(d.prototype.name)) ++
      super.outputs(mole)

}
