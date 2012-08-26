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

  def apply[G <: GAGenome, MF](
    name: String,
    archive: IPrototype[Population[G, MF]],
    filtered: IPrototype[Population[G, MF]])(implicit plugins: IPluginSet) =
    new TaskBuilder { builder ⇒

      addInput(archive)
      addInput(filtered)
      addOutput(archive)

      def toTask = new FilterPopulationTask(name, archive, filtered) {
        val inputs = builder.inputs
        val outputs = builder.outputs
        val parameters = builder.parameters
      }
    }

}

sealed abstract class FilterPopulationTask[G <: GAGenome, MF](
    val name: String,
    archive: IPrototype[Population[G, MF]],
    filtered: IPrototype[Population[G, MF]])(implicit val plugins: IPluginSet) extends Task { task ⇒

  override def process(context: IContext) = {
    val filter = context.valueOrException(filtered).content.map { _.genome }.toSet
    new Variable(
      archive,
      context.valueOrException(archive).content.filterNot(e ⇒ filter.contains(e.genome)): Population[G, MF])
  }

}
