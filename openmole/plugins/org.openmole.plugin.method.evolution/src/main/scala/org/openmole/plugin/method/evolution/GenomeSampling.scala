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
import org.openmole.core.implementation.sampling.Sampling
import org.openmole.core.model.data._
import DataModeMask._
import org.openmole.misc.exception._
import org.openmole.misc.tools.service.Random._
import org.openmole.misc.workspace._
import org.openmole.core.implementation.task.Task._

object GenomeSampling {

  def apply(evolution: Evolution)(genome: IPrototype[evolution.G], size: Int) = {
    val (_genome, _size) = (genome, size)
    new GenomeSampling(evolution) {
      val genome = _genome.asInstanceOf[IPrototype[evolution.G]]
      val size = _size
    }
  }

}

sealed abstract class GenomeSampling(val evolution: Evolution) extends Sampling {

  def genome: IPrototype[evolution.G]
  def size: Int
  override def inputs = DataSet(new Data(genome, optional).toArray)

  def prototypes = List(genome)

  def build(context: IContext) = {
    def toSamplingLine(g: evolution.G) = List(new Variable(genome, g))

    val rng = newRNG(context.valueOrException(openMOLESeed))

    val initialGenomes = context.value(genome.toArray).getOrElse(Array.empty).map { toSamplingLine }

    {
      initialGenomes.toList :::
        (0 until size - initialGenomes.size).map(i ⇒ toSamplingLine(evolution.factory.random(rng))).toList
    }.slice(0, size).iterator
  }
}
