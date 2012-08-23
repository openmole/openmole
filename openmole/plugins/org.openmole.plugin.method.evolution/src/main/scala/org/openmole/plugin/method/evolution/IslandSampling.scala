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
import org.openmole.core.implementation.sampling._
import org.openmole.core.model.data._
import org.openmole.misc.tools.service.Random._
import org.openmole.core.implementation.task.Task._

object IslandSampling {

  def apply(evolution: G with GManifest with GenomeFactory)(genome: IPrototype[Array[evolution.G]], islandSize: Int, size: Int) = {
    val (_genome, _islandSize, _size) = (genome, islandSize, size)
    new IslandSampling(evolution) {
      val genome = _genome.asInstanceOf[IPrototype[Array[evolution.G]]]
      val islandSize = _islandSize
      val size = _size
    }
  }

}

sealed abstract class IslandSampling(val evolution: G with GManifest with GenomeFactory) extends Sampling {

  def genome: IPrototype[Array[evolution.G]]
  def islandSize: Int
  def size: Int
  def prototypes = List(genome)

  def build(context: IContext) = {
    import evolution._

    def toSamplingLine(g: Array[evolution.G]) = List(new Variable(genome, g))

    val rng = newRNG(context.valueOrException(openMOLESeed))

    (0 until size).map(
      i ⇒
        toSamplingLine(
          (0 until islandSize).map(
            j ⇒ evolution.genomeFactory.random(rng)).toArray)).iterator
  }
}

