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
import org.openmole.core.workflow.builder.TaskBuilder
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.task._

object ScalingGAPopulationTask {

  def apply(algorithm: GAAlgorithm)(
    population: Prototype[algorithm.Pop]) = {

    val (_population) = (population)

    new TaskBuilder { builder ⇒
      addInput(population)
      (algorithm.inputsPrototypes ++ algorithm.outputPrototypes).distinct foreach { i ⇒ addOutput(i.toArray) }

      def toTask = new ScalingGAPopulationTask(algorithm) with Built {
        val population = _population.asInstanceOf[Prototype[algorithm.Pop]]
      }
    }
  }

}

abstract class ScalingGAPopulationTask(val algorithm: GAAlgorithm) extends Task {

  val population: Prototype[algorithm.Pop]

  override def process(context: Context)(implicit rng: RandomProvider) =
    algorithm.toVariables(context(population), context)

}
