/*
 * Copyright (C) 2012 reuillon
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

import org.openmole.core.model.execution.IEnvironment
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.mole.IMole

import org.openmole.core.model.task.ITask
import puzzle._
import task._

package object mole {
  implicit def capsuleToSlotConverter(capsule: ICapsule) = capsule.defaultInputSlot
  implicit def puzzleToMoleConverter(puzzle: Puzzle) = new Mole(puzzle.first.capsule)
  
  implicit def moleToMoleExecutionConverter(mole: IMole) = new MoleExecution(mole)
  
  
  class PuzzleMoleDecorator(puzzle: Puzzle) {
    def on(env: IEnvironment): Puzzle = 
      puzzle.copy(selection = puzzle.selection + (puzzle.last -> new FixedEnvironmentSelection(env)))     
    def toExecution = new MoleExecution(puzzle, puzzle.selection, puzzle.grouping)
  }
  
  implicit def puzzleMoleDecoraton(puzzle: Puzzle) = new PuzzleMoleDecorator(puzzle)
  
  implicit def capsuleMoleDecoraton(capsule: ICapsule) = new PuzzleMoleDecorator(capsule)
  implicit def taskMoleDecoraton(task: ITask) = new PuzzleMoleDecorator(task)
  implicit def taskMoleBuilderDecoraton(taskBuilder: TaskBuilder) = new PuzzleMoleDecorator(taskBuilder)
  
}
