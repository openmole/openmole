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

import org.openmole.core.tools.service.Random
import org.openmole.core.workflow.builder.TaskBuilder
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import Random._

import fr.iscpif.mgo._

object SamplePopulationTask {

  def apply(evolution: G with P with F)(
    population: Prototype[Population[evolution.G, evolution.P, evolution.F]],
    size: Int)(implicit plugins: PluginSet) = {
    val (_population) = (population)

    new TaskBuilder { builder ⇒
      addInput(population)
      addOutput(population)

      def toTask =
        new SamplePopulationTask(evolution, size) with Built {
          val population = _population.asInstanceOf[Prototype[Population[evolution.G, evolution.P, evolution.F]]]
        }
    }

  }

}

sealed abstract class SamplePopulationTask(
    val evolution: G with P with F,
    val size: Int) extends Task {

  def population: Prototype[Population[evolution.G, evolution.P, evolution.F]]

  override def process(context: Context) = {
    implicit val rng = Task.buildRNG(context)
    val p = context(population)
    val newP = Population((0 until size).map { i ⇒ p(rng.nextInt(p.size)) })
    Variable(population, newP)
  }

}
