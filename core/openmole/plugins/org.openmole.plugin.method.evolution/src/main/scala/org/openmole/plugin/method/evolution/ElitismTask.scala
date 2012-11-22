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

import org.openmole.core.implementation.data._
import org.openmole.core.implementation.task._
import org.openmole.core.model.data._
import org.openmole.core.model.task._

object ElitismTask {

  def apply(evolution: Elitism with Termination with Modifier with Archive)(
    name: String,
    individuals: Prototype[Array[Individual[evolution.G, evolution.F]]],
    population: Prototype[Population[evolution.G, evolution.F, evolution.MF]],
    archive: Prototype[evolution.A],
    generation: Prototype[Int],
    state: Prototype[evolution.STATE],
    terminated: Prototype[Boolean])(implicit plugins: PluginSet) = {
    val (_individuals, _population, _archive, _generation, _state, _terminated) = (individuals, population, archive, generation, state, terminated)

    new TaskBuilder { builder ⇒
      addInput(population)
      addInput(archive)
      addInput(individuals)
      addInput(generation)
      addInput(state)
      addOutput(population)
      addOutput(archive)
      addOutput(generation)
      addOutput(state)
      addOutput(terminated)

      addParameter(population -> Population.empty)
      addParameter(archive -> evolution.initialArchive)
      addParameter(generation -> 0)
      addParameter(new DynamicParameter(state, evolution.initialState(Population.empty)))

      def toTask = new ElitismTask(name, evolution) {

        val individuals = _individuals.asInstanceOf[Prototype[Array[Individual[evolution.G, evolution.F]]]]
        val population = _population.asInstanceOf[Prototype[Population[evolution.G, evolution.F, evolution.MF]]]
        val archive = _archive.asInstanceOf[Prototype[evolution.A]]
        val generation = _generation
        val state = _state.asInstanceOf[Prototype[evolution.STATE]]
        val terminated = _terminated

        val inputs = builder.inputs
        val outputs = builder.outputs
        val parameters = builder.parameters
      }
    }
  }
}

sealed abstract class ElitismTask[E <: Elitism with Termination with Modifier](
    val name: String, val evolution: E)(implicit val plugins: PluginSet) extends Task {

  def individuals: Prototype[Array[Individual[evolution.G, evolution.F]]]
  def population: Prototype[Population[evolution.G, evolution.F, evolution.MF]]
  def archive: Prototype[evolution.A]
  def state: Prototype[evolution.STATE]
  def generation: Prototype[Int]
  def terminated: Prototype[Boolean]

  override def process(context: Context) = {
    val currentPopulation = context.valueOrException(population) //.asInstanceOf[Population[evolution.G, evolution.F, evolution.MF]]
    val globalPopulation = context.valueOrException(individuals).toList ::: currentPopulation.toIndividuals.toList
    val currentArchive = context.valueOrException(archive)

    val (computedPopulation, computedArchive) = evolution.toPopulation(globalPopulation, currentArchive)
    val newPopulation = evolution.elitism(computedPopulation)

    val (term, newState) = evolution.terminated(
      newPopulation,
      context.valueOrException(state))

    val terminatedVariable = Variable(terminated, term)
    val newStateVariable = Variable(state, newState)
    Context(
      Variable(population, computedPopulation),
      Variable(archive, computedArchive),
      terminatedVariable,
      newStateVariable,
      Variable(generation, context.valueOrException(generation) + 1))
  }

}
