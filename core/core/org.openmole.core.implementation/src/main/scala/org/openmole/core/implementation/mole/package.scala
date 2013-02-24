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

  class PuzzleDecorator(puzzle: Puzzle) {
    def on(env: Environment) =
      puzzle.copy(selection = puzzle.selection ++ puzzle.lasts.map(_ -> new FixedEnvironmentSelection(env)))
    def hook(hooks: IHook*) =
      puzzle.copy(hooks = puzzle.hooks.toList ::: puzzle.lasts.flatMap(c ⇒ hooks.map(c -> _)).toList)
    def source(sources: ISource*) =
      puzzle.copy(sources = puzzle.sources.toList ::: puzzle.lasts.flatMap(c ⇒ sources.map(c -> _)).toList)
  }

  implicit def puzzleMoleExecutionDecoration(puzzle: Puzzle) = new PuzzleDecorator(puzzle)
  implicit def capsuleMoleExecutionDecoration(capsule: ICapsule) = new PuzzleDecorator(capsule.toPuzzle)
  implicit def slotPuzzleDecoration(slot: Slot) = new PuzzleDecorator(slot.toPuzzle)
  implicit def taskMoleExecutionDecoration(task: ITask): PuzzleDecorator = new PuzzleDecorator(task.toCapsule.toPuzzle)
  implicit def taskMoleBuilderDecoration(taskBuilder: TaskBuilder) = new PuzzleDecorator(taskBuilder.toTask.toCapsule.toPuzzle)
  implicit def environmentToFixedEnvironmentSelectionConverter(env: Environment) = new FixedEnvironmentSelection(env)

  implicit def puzzleMoleExecutionConverter(puzzle: Puzzle) = puzzle.toExecution
  implicit def puzzleMoleConverter(puzzle: Puzzle) = puzzle.toMole
  implicit def moleToMoleExecutionConverter(mole: IMole) = new MoleExecution(mole)

  implicit def hookBuilderToHookConverter(hb: HookBuilder) = hb.toHook
  implicit def sourceBuilderToSourceConverter(sb: SourceBuilder) = sb.toSource

}
