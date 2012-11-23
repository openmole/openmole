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

import org.openmole.core.implementation.data._
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.task.Task._
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.misc.tools.service.Random._

import fr.iscpif.mgo._

object SelectPopulationTask {

  def apply(evolution: Modifier with G with MF with Selection with Lambda)(
    name: String,
    population: Prototype[Population[evolution.G, evolution.F, evolution.MF]],
    archive: Prototype[evolution.A])(implicit plugins: PluginSet) = {
    val (_archive, _population) = (archive, population)

    new TaskBuilder { builder ⇒
      addInput(archive)
      addInput(population)
      addOutput(archive)
      addOutput(population)

      def toTask =
        new SelectPopulationTask(name, evolution) {
          val population = _population.asInstanceOf[Prototype[Population[evolution.G, evolution.F, evolution.MF]]]
          val archive = _archive.asInstanceOf[Prototype[evolution.A]]
          val inputs = builder.inputs
          val outputs = builder.outputs
          val parameters = builder.parameters
        }
    }

  }

}

sealed abstract class SelectPopulationTask(
    val name: String,
    val evolution: Modifier with G with MF with F with Selection with Lambda)(implicit val plugins: PluginSet) extends Task {

  def population: Prototype[Population[evolution.G, evolution.F, evolution.MF]]
  def archive: Prototype[evolution.A]

  override def process(context: Context) = {
    implicit val rng = newRNG(context.valueOrException(openMOLESeed))
    val p = context.valueOrException(population)
    val a = context.valueOrException(archive)
    val newP = evolution.toPopulation((0 until evolution.lambda).map { i ⇒ evolution.selection(p) }, a)
    context + Variable(population, newP) + Variable(archive, a)
  }
}
