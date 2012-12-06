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
    archive: Prototype[evolution.A],
    generation: Prototype[Int],
    state: Prototype[evolution.STATE],
    terminated: Prototype[Boolean])(implicit plugins: PluginSet) = {
    val (_individuals, _archive, _generation, _state, _terminated) = (individuals, archive, generation, state, terminated)

    new TaskBuilder { builder ⇒
      addInput(archive)
      addInput(individuals)
      addInput(generation)
      addInput(state)
      addOutput(individuals)
      addOutput(archive)
      addOutput(generation)
      addOutput(state)
      addOutput(terminated)

      def toTask = new ElitismTask(name, evolution) {

        val individuals = _individuals.asInstanceOf[Prototype[Array[Individual[evolution.G, evolution.F]]]]
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

sealed abstract class ElitismTask[E <: Elitism with Termination with Modifier with Archive](
    val name: String, val evolution: E)(implicit val plugins: PluginSet) extends Task {

  def individuals: Prototype[Array[Individual[evolution.G, evolution.F]]]
  def archive: Prototype[evolution.A]

  def state: Prototype[evolution.STATE]
  def generation: Prototype[Int]
  def terminated: Prototype[Boolean]

  override def process(context: Context) = {
    val a = context(archive)
    val newIndividuals = evolution.elitism(context(individuals), a)

    val (term, newState) =
      evolution.terminated(
        evolution.toPopulation(newIndividuals, a),
        context(state))

    Context(
      Variable(individuals, newIndividuals.toArray),
      Variable(terminated, term),
      Variable(state, newState),
      Variable(generation, context(generation) + 1))
  }

}
