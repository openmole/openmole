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
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.domain._

object ScalingGenomeTask {

  def apply[T <: Algorithm](algorithm: T)(genome: Prototype[algorithm.G])(implicit toVariable: WorkflowIntegration[T]) = {

    val (_genome) = (genome)
    new TaskBuilder { builder ⇒
      toVariable.inputsPrototypes(algorithm) foreach { p ⇒ addOutput(p) }
      addInput(genome)
      addOutput(genome)

      def toTask = new ScalingGenomeTask(algorithm) with Built {
        val genome = _genome.asInstanceOf[Prototype[algorithm.G]]
      }
    }
  }

}

abstract class ScalingGenomeTask[T <: Algorithm](val algorithm: T)(implicit toVariable: WorkflowIntegration[T]) extends Task {
  val genome: Prototype[algorithm.G]

  override def process(context: Context)(implicit rng: RandomProvider) =
    context ++ toVariable.genomeToVariables(algorithm)(context(genome), context)

}
