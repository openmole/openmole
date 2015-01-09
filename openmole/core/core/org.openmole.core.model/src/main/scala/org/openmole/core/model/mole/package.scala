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

package org.openmole.core.model

import org.openmole.core.model.builder._
import org.openmole.core.model.execution._
import org.openmole.core.model.execution.local._
import org.openmole.core.model.puzzle._
import org.openmole.core.model.task._
import org.openmole.core.model.transition._

package object mole {

  case class Hooks(map: Map[ICapsule, Traversable[IHook]])
  case class Sources(map: Map[ICapsule, Traversable[ISource]])

  implicit def hooksToMap(h: Hooks) = h.map.withDefault(_ ⇒ List.empty)
  implicit def mapToHooks(m: Map[ICapsule, Traversable[IHook]]) = new Hooks(m)

  implicit def sourcesToMap(s: Sources) = s.map.withDefault(_ ⇒ List.empty)
  implicit def mapToSources(m: Map[ICapsule, Traversable[ISource]]) = new Sources(m)

  object Hooks {
    def empty = Map.empty[ICapsule, Traversable[IHook]]
  }

  object Sources {
    def empty = Map.empty[ICapsule, Traversable[ISource]]
  }

  implicit def default = LocalEnvironment.default
  implicit lazy val local = ExecutionContext(System.out, None)

  implicit def slotToCapsuleConverter(slot: Slot) = slot.capsule

  class PuzzleDecorator(puzzle: Puzzle) {
    def on(env: Environment) =
      puzzle.copy(environments = puzzle.environments ++ puzzle.lasts.map(_ -> env))
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

  implicit def puzzleMoleExecutionConverter(puzzle: Puzzle) = puzzle.toExecution
  implicit def puzzleMoleConverter(puzzle: Puzzle) = puzzle.toMole
  implicit def moleToMoleExecutionConverter(mole: IMole) = MoleExecution(mole)

  implicit def hookBuilderToHookConverter(hb: HookBuilder) = hb.toHook
  implicit def sourceBuilderToSourceConverter(sb: SourceBuilder) = sb.toSource

}
