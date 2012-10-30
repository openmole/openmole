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

import org.openmole.core.model.execution._
import org.openmole.core.model.job._
import org.openmole.core.model.mole._
import org.openmole.core.model.task._
import org.openmole.core.model.transition._

import transition._
import puzzle._
import task._
import data._

package object mole {
  implicit def slotToCapsuleConverter(slot: Slot) = slot.capsule

  class PuzzleMoleExecutionDecorator(puzzle: Puzzle) {
    def on(env: Environment) =
      puzzle.copy(selection = puzzle.selection ++ puzzle.lasts.map(_ -> new FixedEnvironmentSelection(env)))
    def hook(hook: Hook) =
      puzzle.copy(hooks = puzzle.hooks.toList ::: puzzle.lasts.map(_ -> hook).toList)
  }

  implicit def hookToProfilerConverter(hook: Hook) = new Profiler {
    override def process(job: IMoleJob) = hook.process(job)
  }
  implicit def puzzleMoleExecutionDecoration(puzzle: Puzzle) = new PuzzleMoleExecutionDecorator(puzzle)
  implicit def capsuleMoleExecutionDecoration(capsule: ICapsule) = puzzleMoleExecutionDecoration(capsule.toPuzzle)
  implicit def taskMoleExecutionDecoration(task: ITask): PuzzleMoleExecutionDecorator = capsuleMoleExecutionDecoration(task.toCapsule)
  implicit def taskMoleBuilderDecoraton(taskBuilder: TaskBuilder) = taskMoleExecutionDecoration(taskBuilder.toTask)
  implicit def environmentToFixedEnvironmentSelectionConverter(env: Environment) = new FixedEnvironmentSelection(env)

  implicit def puzzleMoleExecutionConverter(puzzle: Puzzle) = puzzle.toExecution
  implicit def puzzleMoleConverter(puzzle: Puzzle) = puzzle.toMole
  implicit def moleToMoleExecutionConverter(mole: IMole) = new MoleExecution(mole)

}
