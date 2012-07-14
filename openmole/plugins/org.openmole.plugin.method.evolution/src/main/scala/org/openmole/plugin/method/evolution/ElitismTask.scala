/*
 * Copyright (C) 2012 reuillon
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

import fr.iscpif.mgo.Individual
import fr.iscpif.mgo.elitism._
import fr.iscpif.mgo.ga._
import fr.iscpif.mgo._
import fr.iscpif.mgo.diversity._
import fr.iscpif.mgo.ranking._

import fr.iscpif.mgo.termination.Termination
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.task._
import org.openmole.core.model.data._
import org.openmole.core.model.task.IPluginSet

object ElitismTask {

  def apply(evolution: Evolution with Elitism with Termination)(
    name: String,
    individuals: IPrototype[Array[Individual[evolution.G]]],
    archive: IPrototype[Population[evolution.G, evolution.MF]],
    generation: IPrototype[Int],
    state: IPrototype[evolution.STATE],
    terminated: IPrototype[Boolean])(implicit plugins: IPluginSet) = {
    val (_individuals, _archive, _generation, _state, _terminated) = (individuals, archive, generation, state, terminated)

    new TaskBuilder { builder ⇒

      addInput(archive)
      addInput(individuals)
      addInput(generation)
      addInput(state)
      addOutput(archive)
      addOutput(generation)
      addOutput(state)

      addParameter(archive -> evolution.emptyPopulation)
      addParameter(generation -> 0)
      addParameter(state -> evolution.initialState)

      def toTask = new ElitismTask(name, evolution) {

        val individuals = _individuals.asInstanceOf[IPrototype[Array[Individual[evolution.G]]]]
        val archive = _archive.asInstanceOf[IPrototype[Population[evolution.G, evolution.MF]]]
        val generation = _generation
        val state = _state.asInstanceOf[IPrototype[evolution.STATE]]
        val terminated = _terminated

        val inputs = builder.inputs
        val outputs = builder.outputs
        val parameters = builder.parameters
      }
    }
  }
}

sealed abstract class ElitismTask[E <: Evolution with Elitism with Termination](
    val name: String, val evolution: Evolution with Elitism with Termination)(implicit val plugins: IPluginSet) extends Task {

  def individuals: IPrototype[Array[Individual[evolution.G]]]
  def archive: IPrototype[Population[evolution.G, evolution.MF]]
  def state: IPrototype[evolution.STATE]
  def generation: IPrototype[Int]
  def terminated: IPrototype[Boolean]

  override def process(context: IContext) = {
    val currentArchive = context.valueOrException(archive).asInstanceOf[Population[evolution.G, evolution.MF]]
    val globalArchive = context.valueOrException(individuals).toList ::: currentArchive.individuals.toList

    val population = evolution.toPopulation(globalArchive.toIndexedSeq)
    val newArchive = evolution.elitism(population)

    val (term, newState) = evolution.terminated(
      currentArchive,
      newArchive,
      context.valueOrException(state))

    val terminatedVariable = new Variable(terminated, term)
    val newStateVariable = new Variable(state, newState)
    Context(
      new Variable(archive, newArchive),
      terminatedVariable,
      newStateVariable,
      new Variable(generation, context.valueOrException(generation) + 1))
  }

}
