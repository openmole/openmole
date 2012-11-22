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

import fr.iscpif.mgo._
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.task._
import org.openmole.core.model.data._
import org.openmole.core.model.task._

object FilterPopulationTask {

  def apply[G, F, MF](
    name: String,
    population: Prototype[Population[G, F, MF]],
    filtered: Prototype[Population[G, F, MF]])(implicit plugins: PluginSet) =
    new TaskBuilder { builder ⇒

      addInput(population)
      addInput(filtered)
      addOutput(population)

      def toTask = new FilterPopulationTask(name, population, filtered) {
        val inputs = builder.inputs
        val outputs = builder.outputs
        val parameters = builder.parameters
      }
    }

}

sealed abstract class FilterPopulationTask[G, F, MF](
    val name: String,
    population: Prototype[Population[G, F, MF]],
    filtered: Prototype[Population[G, F, MF]])(implicit val plugins: PluginSet) extends Task { task ⇒

  override def process(context: Context) = {
    val filter = context.valueOrException(filtered).content.map { _.genome }.toSet
    Variable(
      population,
      context.valueOrException(population).content.filterNot(e ⇒ filter.contains(e.genome)): Population[G, F, MF])
  }

}
