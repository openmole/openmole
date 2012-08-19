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
import fr.iscpif.mgo.ga._
import fr.iscpif.mgo.ranking._
import fr.iscpif.mgo.breed.Breeding
import fr.iscpif.mgo.diversity._

import org.openmole.core.implementation.data._
import org.openmole.core.implementation.task.Task
import org.openmole.core.implementation.task.TaskBuilder
import org.openmole.core.model.data.DataModeMask
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.task.IPluginSet
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.tools.service.Random._
import org.openmole.core.implementation.task.Task._

object SteadyBreedTask {

  def apply(evolution: Breeding with GManifest)(
    name: String,
    archive: IPrototype[Population[evolution.G, evolution.MF]],
    genome: IPrototype[Array[evolution.G]])(implicit plugins: IPluginSet) = {

    val (_archive, _genome) = (archive, genome)

    new TaskBuilder { builder ⇒
      addInput(archive)
      addOutput(new Data(genome, DataModeMask.explore))

      def toTask =
        new SteadyBreedTask(name, evolution) {
          val archive = _archive.asInstanceOf[IPrototype[Population[evolution.G, evolution.MF]]]
          val genome = _genome.asInstanceOf[IPrototype[Array[evolution.G]]]

          val inputs = builder.inputs
          val outputs = builder.outputs
          val parameters = builder.parameters
        }
    }
  }
}

sealed abstract class SteadyBreedTask(
    val name: String,
    val evolution: Breeding with GManifest)(implicit val plugins: IPluginSet) extends Task {

  def archive: IPrototype[Population[evolution.G, evolution.MF]]
  def genome: IPrototype[Array[evolution.G]]

  override def process(context: IContext) = {
    import evolution._

    val rng = newRNG(context.valueOrException(openMOLESeed))
    val a = context.valueOrException(archive)
    val newGenome = evolution.breed(a)(rng).toArray
    context + new Variable(genome, newGenome)
  }

}
