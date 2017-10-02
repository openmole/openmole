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

import fr.iscpif.scalabc.algorithm.Lenormand
import org.openmole.core.context.Val
import org.openmole.core.expansion.{ Condition, FromContext }
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._

package object abc {

  trait ABCPuzzle {
    def iteration: Val[Int]
    def algorithm: ABC
  }

  def abc(
    algorithm: Lenormand with ABC,
    model:     Puzzle
  ) = {
    val methodName = "abc"
    val acceptedPrototype = Val[Double](methodName + "Accepted")
    val iterationPrototype = Val[Int](methodName + "Iteration")
    val statePrototype = Val[Lenormand#STATE](methodName + "State")
    val terminatedPrototype = Val[Boolean](methodName + "Terminated")
    val preModel = StrainerCapsule(EmptyTask() set (name := methodName + "PreModel"))
    val postModel = Slot(StrainerCapsule(EmptyTask() set (name := methodName + "PostModel")))
    val last = StrainerCapsule(EmptyTask() set (name := methodName + "Last"))

    val sampling = LenormandSampling(algorithm, statePrototype)
    val explorationTask = ExplorationTask(sampling) set (
      name := methodName + "Exploration",
      statePrototype := FromContext(_ ⇒ algorithm.initialState),
      outputs += statePrototype
    )

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

    val terminated: Condition = terminatedPrototype

    val modelVariables = algorithm.priorPrototypes ++ algorithm.targetPrototypes

    val puzzle =
      (exploration -< (preModel filter Block(statePrototype)) -- model -- postModel >- analyse -- (last when terminated)) &
        (exploration -- (analyse filter Block(modelVariables: _*))) &
        (preModel -- postModel) &
        (exploration oo (model.firstSlot, filter = Block(modelVariables: _*))) &
        (analyse -- (exploration when !terminated filter Block(modelVariables: _*)))

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
