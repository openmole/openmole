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

import org.openmole.core.preference.Preference
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.workflow.execution._
import org.openmole.core.workflow.mole._
import org.openmole.core.workspace.NewFile
import org.openmole.tool.random.Seeder

trait PuzzleContainer {
  def buildPuzzle: Puzzle
  def toExecution(implicit moleServices: MoleServices) = buildPuzzle.toExecution
}

case class OutputPuzzleContainer(
  puzzle: Puzzle,
  output: Capsule,
  hooks:  Seq[Hook] = Seq.empty
) extends HookDecorator[OutputPuzzleContainer] with PuzzleContainer {
  def hook(hs: Hook*) = copy(hooks = hooks ++ hs)
  def buildPuzzle = puzzle.copy(hooks = puzzle.hooks ++ hooks.map(output → _))
}

case class OutputEnvironmentPuzzleContainer(
  puzzle:      Puzzle,
  output:      Capsule,
  delegate:    Capsule,
  hooks:       Seq[Hook]                   = Seq.empty,
  environment: Option[EnvironmentProvider] = None,
  grouping:    Option[Grouping]            = None
) extends HookDecorator[OutputEnvironmentPuzzleContainer] with EnvironmentDecorator[OutputEnvironmentPuzzleContainer] with PuzzleContainer {

  def on(environment: EnvironmentProvider) = copy(environment = Some(environment))
  def by(strategy: Grouping): OutputEnvironmentPuzzleContainer = copy(grouping = Some(strategy))
  def hook(hs: Hook*) = copy(hooks = hooks ++ hs)

  def buildPuzzle: Puzzle =
    puzzle.copy(
      hooks = puzzle.hooks ++ hooks.map(output → _),
      environments = puzzle.environments ++ environment.map(delegate → _),
      grouping = puzzle.grouping ++ grouping.map(delegate → _)
    )
}