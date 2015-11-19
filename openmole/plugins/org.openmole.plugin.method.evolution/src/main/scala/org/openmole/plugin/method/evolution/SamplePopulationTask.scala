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
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.builder._

object SamplePopulationTask {

  def apply(algorithm: Algorithm)(
    population: Prototype[algorithm.Pop],
    sample: Int,
    size: Int) = {
    val (_population) = (population)

    new TaskBuilder {
      builder ⇒
      addInput(population)
      addExploredOutput(population.toArray)

      def toTask =
        new SamplePopulationTask(algorithm, sample, size) with Built {
          val population = _population.asInstanceOf[Prototype[algorithm.Pop]]
        }
    }
  }

}

abstract class SamplePopulationTask(
    val algorithm: Algorithm,
    val sample: Int,
    val size: Int) extends Task {

  def population: Prototype[algorithm.Pop]

  override def process(context: Context)(implicit rng: RandomProvider) = {
    val p = context(population)

    def samples =
      if (p.isEmpty) Vector.empty
      else Vector.fill(sample) { p(rng().nextInt(p.size)) }

    def populations = Array.fill(size)(samples)
    Variable(population.toArray, populations)
  }

}

