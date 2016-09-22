/*
 * Copyright (C) 2012 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

package org.openmole.plugin.method

import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.validation.DataflowProblem.MissingInput
import org.openmole.core.workflow.validation.{ DataflowProblem, Validation }
import org.openmole.plugin.tool.pattern._

package object stochastic {

  def Replicate(
    model:       Puzzle,
    sampling:    Sampling,
    aggregation: Puzzle
  ): Puzzle = {
    val explorationSkel = ExplorationTask(sampling) set (
      name := "replicateExploration"
    )

    val missing =
      Validation(explorationSkel -< model).collect {
        case MissingInput(_, d) ⇒ d
      }

    val exploration = explorationSkel set ((inputs, outputs) += (missing: _*))
    val explorationCapsule = StrainerCapsule(exploration)
    Strain(explorationCapsule -< model >- aggregation)
  }

  def Replicate(
    model:    Puzzle,
    sampling: Sampling
  ) = {

    val explorationSkel = ExplorationTask(sampling) set (
      name := "replicateExploration"
    )

    val missing =
      Validation(explorationSkel -< model).collect {
        case MissingInput(_, d) ⇒ d
      }

    val exploration = explorationSkel set ((inputs, outputs) += (missing: _*))
    val aggregation = EmptyTask() set (name := "replicateAggregation")

    val explorationCapsule = StrainerCapsule(exploration)
    val aggregationCapsule = Slot(aggregation)
    Strain(explorationCapsule -< model >- aggregationCapsule)
  }

}
