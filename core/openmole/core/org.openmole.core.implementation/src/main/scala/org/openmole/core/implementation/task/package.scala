/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.core.implementation

import org.openmole.core.implementation.mole.Capsule
import org.openmole.core.implementation.puzzle.Puzzle
import org.openmole.core.model.task.ITask
import org.openmole.misc.pluginmanager.PluginManager
import mole._
import puzzle._
import java.io.File

package object task {
  implicit def taskBuilderToTask[TB <: TaskBuilder](builder: TB) = builder.toTask
  implicit def taskToCapsuleConverter(task: ITask) = new Capsule(task)
  implicit def taskBuilderToCapsuleConverter[TB <: TaskBuilder](builder: TB) = new Capsule(builder)

  implicit def taskToPuzzleConverter(task: ITask) = new Capsule(task).toPuzzle

  class TaskToCapsuleDecorator(task: ITask) {
    def toCapsule = new Capsule(task)
    def toStrainerCapsule = new StrainerCapsule(task)
  }

  implicit def taskToCapsuleDecorator(task: ITask) = new TaskToCapsuleDecorator(task)
  implicit def taskBuilderToCapsuleDecorator(task: TaskBuilder) = taskToCapsuleDecorator(task)
  implicit def taskBuilderToPuzzleConverter(t: TaskBuilder) = t.toTask.toCapsule.toPuzzle

}