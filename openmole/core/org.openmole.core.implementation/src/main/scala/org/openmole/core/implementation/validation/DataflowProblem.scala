/*
 * Copyright (C) 2012 reuillon
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

import org.openmole.core.model.data.IData
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.task.ITask
import org.openmole.core.model.transition.ISlot

object DataflowProblem {
  case class WrongType(
      val capsule: ICapsule,
      val slot: ISlot,
      val data: IData[_],
      val provided: IPrototype[_]) extends DataflowProblem {

    override def toString = "Wrong type from capsule " + capsule + " to " + slot + ", data " + data.prototype + " is expected but " + provided + " is provided."
  }

  case class MissingInput(
      val capsule: ICapsule,
      val slot: ISlot,
      val data: IData[_]) extends DataflowProblem {

    override def toString = "Input " + data + " is missing when reaching the " + slot + "."
  }
}

trait DataflowProblem extends Problem {
  def capsule: ICapsule
  def slot: ISlot
  def data: IData[_]

}
