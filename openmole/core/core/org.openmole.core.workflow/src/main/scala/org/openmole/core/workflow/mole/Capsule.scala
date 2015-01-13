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
import org.openmole.misc.exception._

object Capsule {

  implicit def taskToCapsuleConverter(task: Task) = new Capsule(task)
  implicit def slotToCapsuleConverter(slot: Slot) = slot.capsule

  def apply(task: Task) = new Capsule(task)
}

/**
 * A capsule containing a task.
 *
 * @param task task inside this capsule
 */
class Capsule(val task: Task) {

  /*
   * Get the inputs data taken by this capsule, generally it is empty if the capsule
   * is empty or the input of the task inside the capsule. It can be different
   * in some cases.
   * 
   * @return the input of the capsule
   */
  def inputs(mole: Mole, sources: Sources, hooks: Hooks): DataSet =
    task.inputs --
      sources(this).flatMap(_.outputs) --
      sources(this).flatMap(_.inputs) ++
      sources(this).flatMap(_.inputs)

  /*
   * Get the outputs data taken by this capsule, generally it is empty if the capsule
   * is empty or the output of the task inside the capsule. It can be different
   * in some cases.
   * 
   * @return the output of the capsule
   */
  def outputs(mole: Mole, sources: Sources, hooks: Hooks): DataSet =
    task.outputs --
      hooks(this).flatMap(_.outputs) ++
      hooks(this).flatMap(_.outputs)

  override def toString = task.toString

}
