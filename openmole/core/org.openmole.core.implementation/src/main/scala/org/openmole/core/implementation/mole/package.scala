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

import org.openmole.core.implementation.transition.Slot
import org.openmole.core.model.execution.IEnvironment
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.mole.IMole

import org.openmole.core.model.task.ITask
import org.openmole.core.model.transition.ISlot
import puzzle._
import task._
import data._

package object mole {
  implicit def slotToCapsuleConverter(slot: ISlot) = slot.capsule
  implicit def capsuleToSlotConverter(capsule: ICapsule) = capsule.defaultInputSlot
  implicit def puzzleToMoleConverter(puzzle: Puzzle) = new Mole(puzzle.first.capsule)

  implicit def moleToMoleExecutionConverter(mole: IMole) = new MoleExecution(mole)

  class PuzzleMoleExecutionDecorator(puzzle: Puzzle) {
    def toExecution = new MoleExecution(new Mole(puzzle.first.capsule), puzzle.selection, puzzle.grouping)
    def on(env: IEnvironment) =
      puzzle.copy(selection = puzzle.selection ++ puzzle.lasts.map(_ -> new FixedEnvironmentSelection(env)))
  }

  implicit def puzzleMoleExecutionDecoration(puzzle: Puzzle) = new PuzzleMoleExecutionDecorator(puzzle)
  implicit def capsuleMoleExecutionDecoration(capsule: ICapsule) = puzzleMoleExecutionDecoration(capsule.toPuzzle)
  implicit def taskMoleExecutionDecoration(task: ITask): PuzzleMoleExecutionDecorator = capsuleMoleExecutionDecoration(task.toCapsule)
  implicit def taskMoleBuilderDecoraton(taskBuilder: TaskBuilder) = taskMoleExecutionDecoration(taskBuilder.toTask)
  implicit def environmentToFixedEnvironmentSelectionConverter(env: IEnvironment) = new FixedEnvironmentSelection(env)

  implicit def caspuleSlotDecorator(capsule: ICapsule) = new {
    def slot(i: Int) = {
      (0 to (i - capsule.intputSlots.size)).foreach { i ⇒ newSlot }
      capsule.intputSlots.toIndexedSeq(i)
    }
    def newSlot = new Slot(capsule)

    def channel(slot: ISlot, filtered: String*) = new DataChannel(capsule, slot, filtered: _*)
  }

}
