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

  def apply(evolution: Elitism with Termination with Archive)(
    population: Prototype[Population[evolution.G, evolution.P, evolution.F]],
    offspring: Prototype[Array[Individual[evolution.G, evolution.P, evolution.F]]],
    archive: Prototype[evolution.A])(implicit plugins: PluginSet) = {
    val (_population, _offspring, _archive) = (population, offspring, archive)

    new TaskBuilder { builder ⇒
      addInput(archive)
      addInput(population)
      addInput(offspring)
      addOutput(population)
      addOutput(archive)

      def toTask = new ElitismTask(evolution) with builder.Built {
        val population = _population.asInstanceOf[Prototype[Population[evolution.G, evolution.P, evolution.F]]]
        val offspring = _offspring.asInstanceOf[Prototype[Array[Individual[evolution.G, evolution.P, evolution.F]]]]

        val archive = _archive.asInstanceOf[Prototype[evolution.A]]
      }
    }
  }
}

sealed abstract class ElitismTask[E <: Elitism with Termination with Archive](val evolution: E) extends Task {

  def population: Prototype[Population[evolution.G, evolution.P, evolution.F]]
  def offspring: Prototype[Array[Individual[evolution.G, evolution.P, evolution.F]]]
  def archive: Prototype[evolution.A]

  override def process(context: Context) = {
    val a = context(archive)
    val rng = Task.buildRNG(context)
    val offspringPopulation = Population.fromIndividuals(context(offspring))

    val newArchive = evolution.archive(a, context(population), offspringPopulation)(rng)
    val newPopulation = evolution.elitism(context(population), offspringPopulation, newArchive)(rng)

    Context(Variable(population, newPopulation), Variable(archive, newArchive))
  }

}
