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

  def apply(evolution: Breeding with GManifest)(
    population: Prototype[Population[evolution.G, evolution.F, evolution.MF]],
    genome: Prototype[evolution.G],
    size: Int)(implicit plugins: PluginSet) = {
    val (_population, _genome) = (population, genome)
    new BreedSampling(evolution, size) {
      val population = _population.asInstanceOf[Prototype[Population[evolution.G, evolution.F, evolution.MF]]]
      val genome = _genome.asInstanceOf[Prototype[evolution.G]]
    }
  }
}

sealed abstract class BreedSampling(val evolution: Breeding with GManifest, val size: Int) extends Sampling {
  def population: Prototype[Population[evolution.G, evolution.F, evolution.MF]]
  def genome: Prototype[evolution.G]

  def prototypes = List(genome)
  override def inputs = DataSet(Data(population, Optional))

  override def build(context: Context) = {
    val rng = newRNG(context.valueOrException(openMOLESeed))
    val p = context.value(population).getOrElse(Population.empty)

    evolution.breed(p, size)(rng).map(g ⇒ List(Variable(genome, g))).toIterator
  }
}
