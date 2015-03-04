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

package org.openmole.core.workflow

import org.openmole.core.macros.Keyword._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.puzzle._

package builder {

  class Inputs {
    def +=(d: Data[_]*) = (_: InputOutputBuilder).addInput(d: _*)
  }

  class Outputs {
    def +=(d: Data[_]*) = (_: InputOutputBuilder).addOutput(d: _*)
  }

  trait BuilderPackage {
    implicit def samplingBuilderToSampling(s: SamplingBuilder) = s.toSampling
    implicit def taskBuilderToTask[TB <: TaskBuilder](builder: TB) = builder.toTask
    implicit def taskBuilderToCapsuleConverter[TB <: TaskBuilder](builder: TB) = Capsule(builder)
    implicit def taskBuilderToCapsuleDecorator(task: TaskBuilder) = new TaskToCapsuleDecorator(task)
    implicit def taskBuilderToPuzzleConverter(t: TaskBuilder) = t.toTask.toCapsule.toPuzzle

    final lazy val inputs: Inputs = new Inputs
    final lazy val outputs: Outputs = new Outputs

    class AssignDefault[T](p: Prototype[T]) {
      def :=[U <: InputOutputBuilder](v: T, `override`: Boolean = false) =
        (_: U).setDefault(p, v, `override`)
    }

    implicit def prototypeToAssignDefault[T](p: Prototype[T]) = new AssignDefault[T](p)

    final lazy val name = set[{ def setName(name: String) }]
  }
}

package object builder extends BuilderPackage
