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

import org.openmole.core.tools.service.Random._
import org.openmole.core.workflow.builder.TaskBuilder
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import fr.iscpif.mgo._
import org.openmole.core.workflow.sampling.Sampling

object BreedTask {

  def apply(algorithm: Algorithm)(
    size: Int,
    population: Prototype[algorithm.Pop],
    state: Prototype[algorithm.AlgorithmState],
    genome: Prototype[algorithm.G]) = {

    val (_population, _state, _genome) = (population, state, genome)

    new TaskBuilder {
      addInput(population)
      addInput(state)
      addOutput(state)
      addExploredOutput(genome)

      override def toTask: Task = new BreedTask(algorithm, size) with Built {
        val population = _population.asInstanceOf[Prototype[algorithm.Pop]]
        val state = _state.asInstanceOf[Prototype[algorithm.AlgorithmState]]
        val genome = _genome.asInstanceOf[Prototype[algorithm.G]]
      }
    }

  }

}

abstract class BreedTask(val algorithm: Algorithm, val size: Int) extends Task {
  def population: Prototype[algorithm.Pop]
  def state: Prototype[algorithm.AlgorithmState]
  def genome: Prototype[algorithm.G]

  override def process(context: Context)(implicit rng: RandomProvider) = {
    val p = context(population)
    val s = context(state)
    val (newState, breeded) = algorithm.breeding(p, size).run(s)

    Context(
      Variable(genome.toArray, breeded.toArray(genome.`type`.manifest)),
      Variable(state, newState)
    )
  }
}
