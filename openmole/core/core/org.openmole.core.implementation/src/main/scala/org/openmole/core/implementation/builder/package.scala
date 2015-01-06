/*
 * Copyright (C) 2014 Romain Reuillon
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
 */

package org.openmole.core.implementation

import org.openmole.misc.macros.Keyword._
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import task._
import mole._
import puzzle._

package object builder {

  implicit def samplingBuilderToSampling(s: SamplingBuilder) = s.toSampling
  implicit def taskBuilderToTask[TB <: TaskBuilder](builder: TB) = builder.toTask
  implicit def taskBuilderToCapsuleConverter[TB <: TaskBuilder](builder: TB) = new Capsule(builder)
  implicit def taskBuilderToCapsuleDecorator(task: TaskBuilder) = new TaskToCapsuleDecorator(task)
  implicit def taskBuilderToPuzzleConverter(t: TaskBuilder) = t.toTask.toCapsule.toPuzzle

  lazy val inputs = add[{ def addInput(d: Data[_]*) }]
  lazy val outputs = add[{ def addOutput(d: Data[_]*) }]

  class AssignDefault[T](p: Prototype[T]) {
    def :=(v: T, `override`: Boolean = false) =
      (_: InputOutputBuilder).setDefault(p, v, `override`)
  }

  lazy val default = new {
    def apply[T](p: Prototype[T]) = new AssignDefault[T](p)
  }

  lazy val name = set[{ def setName(name: String) }]

}
