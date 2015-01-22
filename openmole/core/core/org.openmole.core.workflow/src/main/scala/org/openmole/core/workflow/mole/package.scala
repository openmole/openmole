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

    class PuzzleDecorator(puzzle: Puzzle) {
      def on(env: Environment) =
        puzzle.copy(environments = puzzle.environments ++ puzzle.lasts.map(_ -> env))

      def hook(hooks: Hook*) =
        puzzle.copy(hooks = puzzle.hooks.toList ::: puzzle.lasts.flatMap(c ⇒ hooks.map(c -> _)).toList)

      def source(sources: Source*) =
        puzzle.copy(sources = puzzle.sources.toList ::: puzzle.lasts.flatMap(c ⇒ sources.map(c -> _)).toList)
    }

    implicit def puzzleMoleExecutionDecoration(puzzle: Puzzle) = new PuzzleDecorator(puzzle)

    implicit def capsuleMoleExecutionDecoration(capsule: Capsule) = new PuzzleDecorator(capsule.toPuzzle)

    implicit def slotPuzzleDecoration(slot: Slot) = new PuzzleDecorator(slot.toPuzzle)

    implicit def taskMoleExecutionDecoration(task: Task): PuzzleDecorator = new PuzzleDecorator(task.toCapsule.toPuzzle)

    implicit def taskMoleBuilderDecoration(taskBuilder: TaskBuilder) = new PuzzleDecorator(taskBuilder.toTask.toCapsule.toPuzzle)

    implicit def puzzleMoleExecutionConverter(puzzle: Puzzle) = puzzle.toExecution

    implicit def puzzleMoleConverter(puzzle: Puzzle) = puzzle.toMole

    implicit def capsuleToMoleExecutionConverter(capsule: Capsule) = capsule.toPuzzle.toExecution
    implicit def taskToMoleExecutionConverter(task: Task) = task.toCapsule.toPuzzle.toExecution
    implicit def taskBuilderToMoleExecutionConverter(taskBuilder: TaskBuilder) = taskBuilder.toCapsule.toPuzzle.toExecution

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