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

import org.openmole.core.implementation.data._
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.task.Task._
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.misc.tools.service.Random._

import fr.iscpif.mgo._

object SelectPopulationTask {

  def apply(evolution: Modifier with G with MF with Selection with Lambda)(name: String, archive: IPrototype[Population[evolution.G, evolution.MF]])(implicit plugins: IPluginSet) = {
    val (_archive) = (archive)

    new TaskBuilder { builder ⇒
      addInput(archive)
      addOutput(archive)

      def toTask =
        new SelectPopulationTask(name, evolution) {
          val archive = _archive.asInstanceOf[IPrototype[Population[evolution.G, evolution.MF]]]
          val inputs = builder.inputs
          val outputs = builder.outputs
          val parameters = builder.parameters
        }
    }

  }

}

sealed abstract class SelectPopulationTask(val name: String, val evolution: Modifier with G with MF with Selection with Lambda)(implicit val plugins: IPluginSet) extends Task {

  def archive: IPrototype[Population[evolution.G, evolution.MF]]

  override def process(context: IContext) = {
    implicit val rng = newRNG(context.valueOrException(openMOLESeed))
    val p = context.valueOrException(archive)
    context + new Variable(archive, evolution.toPopulation((0 until evolution.lambda).map { i ⇒ evolution.selection(p) }))
  }
}
