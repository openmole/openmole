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

object ScalingGAGenomeTask {

  def apply(evolution: GAAlgorithm)(
    genome: Prototype[evolution.G])(implicit plugins: PluginSet) = {

    val (_genome) = (genome)
    new TaskBuilder { builder ⇒
      evolution.inputsPrototypes foreach { p ⇒ addOutput(p) }
      addInput(genome)
      addOutput(genome)

      def toTask = new ScalingGAGenomeTask(evolution) with Built {
        val genome = _genome.asInstanceOf[Prototype[evolution.G]]
      }
    }
  }

}

sealed abstract class ScalingGAGenomeTask(val evolution: GAAlgorithm) extends Task {
  val genome: Prototype[evolution.G]

  override def process(context: Context) = {
    context ++ evolution.toVariables(context(genome), context)
  }
}
