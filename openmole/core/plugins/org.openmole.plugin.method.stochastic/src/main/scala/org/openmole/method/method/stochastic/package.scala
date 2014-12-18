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

import org.openmole.core.implementation.mole._
import org.openmole.core.implementation.puzzle._
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.tools._
import org.openmole.core.implementation.transition._
import org.openmole.core.implementation.data._
import org.openmole.core.model.data._
import org.openmole.core.model.mole._
import org.openmole.core.model.sampling._
import org.openmole.core.model.task._
import org.openmole.core.model.transition._
import org.openmole.plugin.task.statistics._
import org.openmole.core.implementation.validation.Validation
import org.openmole.core.implementation.validation.DataflowProblem.MissingInput

package object stochastic extends StatisticsMethods {

  def StatisticsTask = org.openmole.plugin.task.statistics.StatisticsTask

  def Replicate(
    name: String,
    model: Puzzle,
    replicationFactor: DiscreteFactor[_, _],
    statisticTask: ITask)(implicit plugins: PluginSet): Puzzle = {
    val exploration = ExplorationTask(name + "Replication", replicationFactor)

    Validation(exploration -< model) foreach {
      case MissingInput(_, d) ⇒
        exploration.addInput(d)
        exploration.addOutput(d)
      case _ ⇒
    }

    val explorationCapsule = StrainerCapsule(exploration)
    val endCapsule = Slot(StrainerCapsule(EmptyTask(name + "End")))

    explorationCapsule -< model >- statisticTask -- endCapsule
  }

  def Replicate(
    name: String,
    model: Puzzle,
    replications: Sampling)(implicit plugins: PluginSet) = {
    val exploration = ExplorationTask(name + "Replication", replications)

    Validation(exploration -< model) foreach {
      case MissingInput(_, d) ⇒
        exploration.addInput(d)
        exploration.addOutput(d)
      case _ ⇒
    }

    val explorationCapsule = StrainerCapsule(exploration)
    val aggregationCapsule = Slot(StrainerCapsule(EmptyTask(name + "Aggregation")))
    explorationCapsule -< model >- aggregationCapsule //+ explorationCapsule oo aggregationCapsule
  }

}
