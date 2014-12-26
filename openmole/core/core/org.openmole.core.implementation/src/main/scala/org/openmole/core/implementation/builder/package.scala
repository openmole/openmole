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

import org.openmole.core.model.data._
import org.openmole.core.model.task.ITask
import task._
import mole._
import puzzle._

package object builder {

  implicit def samplingBuilderToSampling(s: SamplingBuilder) = s.toSampling
  implicit def taskBuilderToTask[TB <: TaskBuilder](builder: TB) = builder.toTask
  implicit def taskBuilderToCapsuleConverter[TB <: TaskBuilder](builder: TB) = new Capsule(builder)
  implicit def taskBuilderToCapsuleDecorator(task: TaskBuilder) = new TaskToCapsuleDecorator(task)
  implicit def taskBuilderToPuzzleConverter(t: TaskBuilder) = t.toTask.toCapsule.toPuzzle

  type Op[T] = T ⇒ Unit
}
