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

package org.openmole.core.implementation.mole

import org.openmole.core.model.mole._
import org.openmole.core.model.task._
import org.openmole.core.model.data._

object Capsule {
  def apply(task: ITask) = new Capsule(task)
}

class Capsule(val task: ITask) extends ICapsule {
  override def inputs(mole: IMole, sources: Sources, hooks: Hooks): DataSet =
    task.inputs --
      sources(this).flatMap(_.outputs) --
      sources(this).flatMap(_.inputs) ++
      sources(this).flatMap(_.inputs)

  override def outputs(mole: IMole, sources: Sources, hooks: Hooks): DataSet =
    task.outputs --
      hooks(this).flatMap(_.outputs) ++
      hooks(this).flatMap(_.outputs)

  override def toString = task.toString
}
