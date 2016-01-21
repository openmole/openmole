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

object GenerateIslandTask {

  def apply[T](algorithm: T, sample: Option[Int], size: Int, outputPopulationName: String)(implicit wfi: WorkflowIntegration[T]) = {
    val t = wfi(algorithm)

    val outputPopulation = t.populationPrototype.withName(outputPopulationName)
    new TaskBuilder {
      builder ⇒
      addInput(t.populationPrototype)
      addExploredOutput(outputPopulation.toArray)

      abstract class GenerateIslandTask extends Task {

        override def process(context: Context)(implicit rng: RandomProvider) = {
          val p = context(t.populationPrototype)

          def samples =
            if (p.isEmpty) Vector.empty
            else sample match {
              case Some(s) ⇒ Vector.fill(s) { p(rng().nextInt(p.size)) }
              case None    ⇒ p
            }

          def populations = Array.fill(size)(t.operations.migrateToIsland(samples))
          Variable(outputPopulation.toArray, populations)
        }

      }

      def toTask = new GenerateIslandTask with Built
    }
  }

}

