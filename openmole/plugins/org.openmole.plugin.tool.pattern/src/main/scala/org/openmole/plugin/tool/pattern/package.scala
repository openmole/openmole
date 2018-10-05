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
package org.openmole.plugin.tool

import org.openmole.core.context._
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.builder.DefinitionScope
import org.openmole.core.workflow.mole.Capsule
import org.openmole.core.workflow.puzzle.Puzzle
import org.openmole.core.workflow.task.{ EmptyTask, MoleTask }

package object pattern {

  case class Wrapped(evaluationPuzzle: Puzzle, delegate: Vector[Capsule])

  def wrapPuzzle(evaluation: Puzzle, inputVals: Seq[Val[_]], outputVals: Seq[Val[_]], wrap: Boolean = true)(implicit definitionScope: DefinitionScope) =
    if (wrap) {
      val moleCapsule = Capsule(MoleTask(evaluation) set (inputs += (inputVals: _*), outputs += (outputVals: _*)))
      Wrapped(moleCapsule, Vector(moleCapsule))
    }
    else {
      val firstEvaluation = EmptyTask() set ((inputs, outputs) += (inputVals: _*))
      val lastEvaluation = EmptyTask() set ((inputs, outputs) += (outputVals: _*))
      val puzzle = firstEvaluation -- evaluation -- lastEvaluation
      Wrapped(puzzle, Puzzle.capsules(evaluation))
    }

}
