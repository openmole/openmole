/*
 * Copyright (C) 2015 Romain Reuillon
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
package org.openmole.core.workflow.puzzle

import org.openmole.core.workflow.execution._
import org.openmole.core.workflow.mole._

trait HookDecorator[T] {
  def hook(hook: Hook*): T
}

trait EnvironmentDecorator[T] {
  def on(environment: Environment): T
  def by(strategy: Grouping): T
}

trait SourceDecorator[T] {
  def source(sources: Source*): T
}

class PuzzlePieceDecorator(puzzle: PuzzlePiece) extends HookDecorator[PuzzlePiece] with EnvironmentDecorator[PuzzlePiece] with SourceDecorator[PuzzlePiece] {
  def on(env: Environment) =
    puzzle.copy(environment = Some(env))

  def hook(hooks: Hook*) =
    puzzle.copy(hooks = puzzle.hooks.toList ::: hooks.toList)

  def source(sources: Source*) =
    puzzle.copy(sources = puzzle.sources.toList ::: sources.toList)

  def by(strategy: Grouping) =
    puzzle.copy(grouping = Some(strategy))
}

trait OutputHookPuzzle <: HookDecorator[Puzzle] { self: Puzzle ⇒
  def output: Capsule
  def hook(hooks: Hook*) = self.copy(hooks = self.hooks.toList ++ hooks.map(h ⇒ output -> h))
}

trait EnvironmentPuzzle <: EnvironmentDecorator[Puzzle] { self: Puzzle ⇒
  def delegate: Capsule

  override def by(strategy: Grouping): Puzzle = self.copy(grouping = self.grouping + (delegate -> strategy))
  override def on(environment: Environment): Puzzle = self.copy(environments = self.environments + (delegate -> environment))
}