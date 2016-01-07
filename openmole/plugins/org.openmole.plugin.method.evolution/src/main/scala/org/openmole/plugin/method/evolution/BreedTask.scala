/*
 * Copyright (C) 24/11/12 Romain Reuillon
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

package org.openmole.plugin.method.evolution

import org.openmole.core.workflow.builder.TaskBuilder
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import fr.iscpif.mgo._

import scala.util.Random
import scalaz._

object BreedTask {

  def apply[T: WorkflowIntegration](t: T, size: Int)(implicit integration: WorkflowIntegration[T]) = {
    val wfi = integration(t)
    import wfi._

    new TaskBuilder {
      addInput(populationPrototype)
      addInput(statePrototype)
      addOutput(statePrototype)
      addExploredOutput(genomePrototype.toArray)

      abstract class BreedTask extends Task {

        override def process(context: Context)(implicit rng: RandomProvider) = {
          val p = context(populationPrototype)

          if (p.isEmpty) {
            val s = context(statePrototype)
            val (news, gs) = algorithm.run(s, operations.initialGenomes(size))

            Context(
              Variable(genomePrototype.toArray, gs.toArray(genomePrototype.`type`.manifest)),
              Variable(statePrototype, news)
            )
          }
          else {
            val s = context(statePrototype)
            val (newState, breeded) = algorithm.run(s, operations.breeding(size).run(p))

            Context(
              Variable(genomePrototype.toArray, breeded.toArray(genomePrototype.`type`.manifest)),
              Variable(statePrototype, newState)
            )
          }
        }

      }

      override def toTask: Task = new BreedTask with Built

    }
  }

}

