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

package org.openmole.plugin.method.evolution

import fr.iscpif.mgo._
import org.openmole.core.workflow.builder.TaskBuilder

import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._

object ElitismTask {

  def apply[T](t: T)(implicit integration: WorkflowIntegration[T]) = {
    val wfi = integration(t)
    import wfi._

    new TaskBuilder {
      addInput(statePrototype)
      addInput(populationPrototype)
      addInput(offspringPrototype)
      addOutput(populationPrototype)
      addOutput(statePrototype)

      abstract class ElitismTask extends Task {

        override def process(context: Context)(implicit rng: RandomProvider) = {
          val (newState, newPopulation) = algorithm.run(context(statePrototype), operations.elitism.run(context(populationPrototype) ++ context(offspringPrototype)))

          Context(
            Variable(populationPrototype, newPopulation),
            Variable(statePrototype, newState)
          )
        }

      }

      def toTask = new ElitismTask with Built
    }
  }

}

