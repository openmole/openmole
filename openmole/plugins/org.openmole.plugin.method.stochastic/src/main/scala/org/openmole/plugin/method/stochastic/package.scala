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

import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.validation.{ Validation, DataflowProblem }
import org.openmole.plugin.task.statistic._
import DataflowProblem.MissingInput

package object stochastic extends StatisticPackage {

  def StatisticsTask = org.openmole.plugin.task.statistic.StatisticTask

  def Replicate(
    model: Puzzle,
    sampling: Sampling,
    aggregation: Capsule)(implicit plugins: PluginSet): Puzzle = {
    val name = "replicate"

    val exploration = ExplorationTask(sampling) set { _.setName(name + "Exploration") }

    Validation(exploration -< model) foreach {
      case MissingInput(_, d) ⇒
        exploration.addInput(d)
        exploration.addOutput(d)
      case _ ⇒
    }

    val explorationCapsule = StrainerCapsule(exploration)
    val aggregationCapsule = Slot(aggregation)
    explorationCapsule -< model >- aggregationCapsule //+ explorationCapsule oo aggregationCapsule
  }

  def Replicate(
    model: Puzzle,
    sampling: Sampling)(implicit plugins: PluginSet) = {
    val name = "replicate"

    val exploration = ExplorationTask(sampling) set { _.setName(name + "Exploration") }

    Validation(exploration -< model) foreach {
      case MissingInput(_, d) ⇒
        exploration.addInput(d)
        exploration.addOutput(d)
      case _ ⇒
    }

    val explorationCapsule = StrainerCapsule(exploration)
    val aggregationCapsule = Slot(Capsule(EmptyTask() set { _.setName(name + "Aggregation") }))
    explorationCapsule -< model >- aggregationCapsule //+ explorationCapsule oo aggregationCapsule
  }

}
