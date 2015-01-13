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

object TerminationTask {

  def apply(evolution: Termination with Archive)(
    population: Prototype[Population[evolution.G, evolution.P, evolution.F]],
    archive: Prototype[evolution.A],
    generation: Prototype[Int],
    state: Prototype[evolution.STATE],
    terminated: Prototype[Boolean])(implicit plugins: PluginSet) = {
    val (_population, _archive, _generation, _state, _terminated) = (population, archive, generation, state, terminated)

    new TaskBuilder { builder ⇒
      addInput(archive)
      addInput(population)
      addInput(generation)
      addInput(state)
      addOutput(generation)
      addOutput(state)
      addOutput(terminated)

      def toTask = new TerminationTask(evolution) with Built {
        val population = _population.asInstanceOf[Prototype[Population[evolution.G, evolution.P, evolution.F]]]
        val archive = _archive.asInstanceOf[Prototype[evolution.A]]
        val generation = _generation
        val state = _state.asInstanceOf[Prototype[evolution.STATE]]
        val terminated = _terminated
      }
    }
  }
}

sealed abstract class TerminationTask[E <: Termination with Archive](val evolution: E) extends Task {

  def population: Prototype[Population[evolution.G, evolution.P, evolution.F]]
  def archive: Prototype[evolution.A]

  def state: Prototype[evolution.STATE]
  def generation: Prototype[Int]
  def terminated: Prototype[Boolean]

  override def process(context: Context) = {
    val rng = Task.buildRNG(context)

    val (term, newState) =
      evolution.terminated(
        context(population),
        context(state))(rng)

    Context(
      Variable(terminated, term),
      Variable(state, newState),
      Variable(generation, context(generation) + 1))
  }

}
