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

import org.openmole.core.dsl._
import org.openmole.core.setter.DefinitionScope
import org.openmole.core.workflow.task.{ EmptyTask, MoleTask }

package object pattern {

  def wrap(evaluation: DSL, inputVals: Seq[Val[?]], outputVals: Seq[Val[?]], wrap: Boolean = true)(implicit definitionScope: DefinitionScope) =
    if (wrap) {
      val moleTask = MoleTask(evaluation) set (inputs ++= inputVals, outputs ++= outputVals)
      DSLContainer(moleTask, (), delegate = Vector(moleTask))
    }
    else {
      val firstEvaluation = EmptyTask() set ((inputs, outputs) ++= inputVals)
      val lastEvaluation = EmptyTask() set ((inputs, outputs) ++= outputVals)
      val puzzle = Strain(firstEvaluation) -- Capsule(evaluation) -- lastEvaluation
      DSLContainer(puzzle, (), delegate = DSL.delegate(evaluation))
    }

}
