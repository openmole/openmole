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

  def apply(algorithm: Algorithm)(
    population: Prototype[algorithm.Pop],
    offspring: Prototype[algorithm.Pop],
    state: Prototype[algorithm.AlgorithmState]) = {
    val (_population, _offspring, _state) = (population, offspring, state)

    new TaskBuilder { builder ⇒
      addInput(state)
      addInput(population)
      addInput(offspring)
      addOutput(population)
      addOutput(state)

      def toTask = new ElitismTask(algorithm) with builder.Built {
        val population = _population.asInstanceOf[Prototype[algorithm.Pop]]
        val offspring = _offspring.asInstanceOf[Prototype[algorithm.Pop]]
        val state = _state.asInstanceOf[Prototype[algorithm.AlgorithmState]]
      }
    }
  }
}

abstract class ElitismTask(val algorithm: Algorithm) extends Task {

  def population: Prototype[algorithm.Pop]
  def offspring: Prototype[algorithm.Pop]
  def state: Prototype[algorithm.AlgorithmState]

  override def process(context: Context)(implicit rng: RandomProvider) = {
    val (newState, newPopulation) =
      algorithm.elitism(context(population),
        context(offspring)).run(context(state))

    Context(
      Variable(population, newPopulation),
      Variable(state, newState)
    )
  }

}
