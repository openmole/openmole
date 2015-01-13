/*
 * Copyright (C) 2014 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._

object PopulationToIndividualsTask {

  def apply(evolution: G with P with F)(
    population: Prototype[Population[evolution.G, evolution.P, evolution.F]],
    individuals: Prototype[Array[Individual[evolution.G, evolution.P, evolution.F]]])(implicit plugins: PluginSet) = {
    val _population = population
    val _individuals = individuals

    new TaskBuilder { builder ⇒
      addInput(population)
      addOutput(individuals)

      def toTask =
        new PopulationToIndividualsTask(evolution) with Built {
          val population = _population.asInstanceOf[Prototype[Population[evolution.G, evolution.P, evolution.F]]]
          val individuals = _individuals.asInstanceOf[Prototype[Array[Individual[evolution.G, evolution.P, evolution.F]]]]
        }
    }

  }

}

sealed abstract class PopulationToIndividualsTask(val evolution: G with P with F) extends Task {

  def population: Prototype[Population[evolution.G, evolution.P, evolution.F]]
  def individuals: Prototype[Array[Individual[evolution.G, evolution.P, evolution.F]]]

  override def process(context: Context) = Variable(individuals, context(population).toIndividuals.toArray)

}
