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

import org.openmole.core.tools.service.Random._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import util.Random
import org.openmole.core.workflow.task._
import fr.iscpif.mgo._
import org.openmole.core.workflow.sampling.Sampling

import scala.util.Random

object BreedSampling {

  def apply(evolution: Breeding with GManifest with Archive)(
    population: Prototype[Population[evolution.G, evolution.P, evolution.F]],
    archive: Prototype[evolution.A],
    genome: Prototype[evolution.G],
    size: Int)(implicit plugins: PluginSet) = {
    val (_population, _archive, _genome) = (population, archive, genome)
    new BreedSampling(evolution, size) {
      val population = _population.asInstanceOf[Prototype[Population[evolution.G, evolution.P, evolution.F]]]
      val archive = _archive.asInstanceOf[Prototype[evolution.A]]
      val genome = _genome.asInstanceOf[Prototype[evolution.G]]
    }
  }
}

sealed abstract class BreedSampling(val evolution: Breeding with GManifest with Archive, val size: Int) extends Sampling {
  def population: Prototype[Population[evolution.G, evolution.P, evolution.F]]
  def archive: Prototype[evolution.A]
  def genome: Prototype[evolution.G]

  def prototypes = List(genome)
  override def inputs = DataSet(population, archive)

  override def build(context: Context)(implicit rng: Random) = {
    val p = context(population)
    val a = context(archive)
    val breeded = evolution.breed(p, a, size)(rng)
    breeded.map(g ⇒ List(Variable(genome, g))).toIterator
  }
}
