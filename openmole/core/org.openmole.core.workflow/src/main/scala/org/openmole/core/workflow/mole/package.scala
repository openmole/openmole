/*
 * Copyright (C) 20/02/13 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.workflow

import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.execution._
import org.openmole.core.workflow.execution.local._
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._

package mole {

  trait MolePackage {

    implicit lazy val localExecutionContext = ExecutionContext(System.out, None)

    class PuzzlePieceDecorator(puzzle: PuzzlePiece) {
      def on(env: Environment) =
        puzzle.copy(environment = Some(env))

      def hook(hooks: Hook*) =
        puzzle.copy(hooks = puzzle.hooks.toList ::: hooks.toList)

      def source(sources: Source*) =
        puzzle.copy(sources = puzzle.sources.toList ::: sources.toList)

      def by(strategy: Grouping) =
        puzzle.copy(grouping = Some(strategy))
    }

    implicit def puzzlePuzzlePieceDecoration(puzzle: PuzzlePiece) = new PuzzlePieceDecorator(puzzle)
    implicit def capsulePuzzlePieceDecoration(capsule: Capsule) = new PuzzlePieceDecorator(capsule.toPuzzlePiece)
    implicit def slotPuzzlePieceDecoration(slot: Slot) = new PuzzlePieceDecorator(slot.toPuzzlePiece)

    implicit def taskPuzzlePieceDecoration(task: Task): PuzzlePieceDecorator = new PuzzlePieceDecorator(task.toCapsule.toPuzzlePiece)

    implicit def taskMoleBuilderPuzzlePieceDecoration(taskBuilder: TaskBuilder) = new PuzzlePieceDecorator(taskBuilder.toTask.toCapsule.toPuzzlePiece)

    implicit def puzzlePieceMoleExecutionConverter(puzzle: PuzzlePiece) = puzzle.toPuzzle.toExecution
    implicit def puzzlePieceMoleConverter(puzzle: PuzzlePiece) = puzzle.toPuzzle.toMole

    implicit def puzzleMoleExecutionConverter(puzzle: Puzzle) = puzzle.toExecution
    implicit def puzzleMoleConverter(puzzle: Puzzle) = puzzle.toMole

    implicit def capsuleToMoleExecutionConverter(capsule: Capsule): MoleExecution = capsule.toPuzzle.toExecution
    implicit def taskToMoleExecutionConverter(task: Task): MoleExecution = task.toCapsule.toPuzzle.toExecution
    implicit def taskBuilderToMoleExecutionConverter(taskBuilder: TaskBuilder): MoleExecution = taskBuilder.toCapsule.toPuzzle.toExecution
    implicit def moleToMoleExecutionConverter(mole: Mole) = MoleExecution(mole)
  }

}

package object mole extends MolePackage {
  case class Hooks(map: Map[Capsule, Traversable[Hook]])

  case class Sources(map: Map[Capsule, Traversable[Source]])

  implicit def hooksToMap(h: Hooks) = h.map.withDefault(_ ⇒ List.empty)

  implicit def mapToHooks(m: Map[Capsule, Traversable[Hook]]) = new Hooks(m)

  implicit def sourcesToMap(s: Sources) = s.map.withDefault(_ ⇒ List.empty)

  implicit def mapToSources(m: Map[Capsule, Traversable[Source]]) = new Sources(m)

  object Hooks {
    def empty = Map.empty[Capsule, Traversable[Hook]]
  }

  object Sources {
    def empty = Map.empty[Capsule, Traversable[Source]]
  }
}