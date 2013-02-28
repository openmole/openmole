/*
 * Copyright (C) 24/11/12 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.method.evolution

import org.openmole.core.implementation.data._
import org.openmole.core.implementation.task._
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.misc.workspace._
import org.openmole.misc.tools.service.Random._
import org.openmole.core.implementation.task.Task._
import algorithm._
import fr.iscpif.mgo._
import org.openmole.core.model.sampling.Sampling

object BreedSampling {

  def apply(evolution: Breeding with GManifest with Archive)(
    individuals: Prototype[Array[Individual[evolution.G, evolution.P, evolution.F]]],
    archive: Prototype[evolution.A],
    genome: Prototype[evolution.G],
    size: Int)(implicit plugins: PluginSet) = {
    val (_individuals, _archive, _genome) = (individuals, archive, genome)
    new BreedSampling(evolution, size) {
      val individuals = _individuals.asInstanceOf[Prototype[Array[Individual[evolution.G, evolution.P, evolution.F]]]]
      val archive = _archive.asInstanceOf[Prototype[evolution.A]]
      val genome = _genome.asInstanceOf[Prototype[evolution.G]]
    }
  }
}

sealed abstract class BreedSampling(val evolution: Breeding with GManifest with Archive, val size: Int) extends Sampling {
  def individuals: Prototype[Array[Individual[evolution.G, evolution.P, evolution.F]]]
  def archive: Prototype[evolution.A]
  def genome: Prototype[evolution.G]

  def prototypes = List(genome)
  override def inputs = DataSet(individuals, archive)

  override def build(context: Context) = {
    val rng = newRNG(context(openMOLESeed))
    val is = context(individuals)
    val a = context(archive)
    evolution.breed(is.toSeq, a, size)(rng).map(g ⇒ List(Variable(genome, g))).toIterator
  }
}
