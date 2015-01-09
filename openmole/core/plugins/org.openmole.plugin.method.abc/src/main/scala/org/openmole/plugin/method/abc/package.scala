/*
 * Copyright (C) 16/01/14 Romain Reuillon
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

package org.openmole.plugin.method

import org.openmole.plugin.method.abc._
import fr.iscpif.scalabc.algorithm.Lenormand
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._

package object abc {

  trait ABCPuzzle {
    def iteration: Prototype[Int]
    def algorithm: ABC
  }

  def abc(
    algorithm: Lenormand with ABC,
    model: Puzzle)(implicit plugins: PluginSet) = {
    val name = "abc"
    val acceptedPrototype = Prototype[Double](name + "Accepted")
    val iterationPrototype = Prototype[Int](name + "Iteration")
    val statePrototype = Prototype[Lenormand#STATE](name + "State")
    val terminatedPrototype = Prototype[Boolean](name + "Terminated")
    val preModel = StrainerCapsule(EmptyTask() set (_.setName(name + "PreModel")))
    val postModel = Slot(StrainerCapsule(EmptyTask() set (_.setName(name + "PostModel"))))
    val last = StrainerCapsule(EmptyTask() set (_.setName(name + "Last")))

    val sampling = LenormandSampling(algorithm, statePrototype)
    val explorationTask = ExplorationTask(sampling) set (_.setName(name + "Exploration"))
    explorationTask setDefault Default(statePrototype, algorithm.initialState)
    explorationTask addOutput statePrototype

    val exploration = StrainerCapsule(explorationTask)

    val analyseTask =
      LenormandAnalyseTask(
        algorithm,
        statePrototype,
        terminatedPrototype,
        iterationPrototype,
        acceptedPrototype
      )

    val analyse = Slot(StrainerCapsule(analyseTask))

    val terminated = Condition(terminatedPrototype.name + " == true")

    val modelVariables = algorithm.priorPrototypes.map(_.name) ++ algorithm.targetPrototypes.map(_.name)

    val puzzle =
      (exploration -< (preModel, filter = Block(statePrototype.name)) -- model -- postModel >- analyse -- (last, terminated)) +
        (exploration -- (analyse, filter = Block(modelVariables: _*))) +
        (preModel -- postModel) +
        (exploration oo (model.first, filter = Block(modelVariables: _*))) +
        (analyse -- (exploration, !terminated, filter = Block(modelVariables: _*)))

    val _algorithm = algorithm

    new Puzzle(puzzle) with ABCPuzzle {
      val output = analyse
      val state = statePrototype
      val accepted = acceptedPrototype
      val iteration = iterationPrototype
      val algorithm = _algorithm
    }
  }

}
