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
import org.openmole.misc.workspace._
import org.openmole.misc.tools.service.Random._
import org.openmole.core.implementation.task.Task._

object BreedTask {

  def apply(evolution: Breeding with GManifest)(
    name: String,
    archive: Prototype[Population[evolution.G, evolution.MF]],
    genome: Prototype[evolution.G])(implicit plugins: PluginSet) = sized(evolution)(name, archive, genome, None)

  def sized(evolution: Breeding with GManifest)(
    name: String,
    archive: Prototype[Population[evolution.G, evolution.MF]],
    genome: Prototype[evolution.G],
    size: Option[Int])(implicit plugins: PluginSet) = {

    val (_archive, _genome) = (archive, genome)

    new TaskBuilder { builder ⇒
      addInput(archive)
      addOutput(Data(genome toArray, DataModeMask.explore))
      addParameter(archive -> Population.empty)

      def toTask =
        new BreedTask(name, evolution, size) {
          val archive = _archive.asInstanceOf[Prototype[Population[evolution.G, evolution.MF]]]
          val genome = _genome.asInstanceOf[Prototype[evolution.G]]

          val inputs = builder.inputs
          val outputs = builder.outputs
          val parameters = builder.parameters
        }
    }
  }
}

sealed abstract class BreedTask(
    val name: String,
    val evolution: Breeding with GManifest, size: Option[Int])(implicit val plugins: PluginSet) extends Task {

  def archive: Prototype[Population[evolution.G, evolution.MF]]
  def genome: Prototype[evolution.G]

  override def process(context: Context) = {
    import evolution._

    val rng = newRNG(context.valueOrException(openMOLESeed))
    val a = context.valueOrException(archive)
    val newGenome = size match {
      case None ⇒ evolution.breed(a)(rng).toArray
      case Some(s) ⇒ evolution.breed(a, s)(rng).toArray
    }
    context + Variable(genome toArray, newGenome)
  }

}
