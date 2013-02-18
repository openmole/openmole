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
import org.openmole.core.implementation.task.Task
import org.openmole.core.implementation.task.TaskBuilder
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.core.model.sampling._
import org.openmole.core.model.domain._

import scala.collection.mutable.ListBuffer
import org.openmole.core.implementation.tools.VariableExpansion

object ScalingGAGenomeTask {

  def scaled(scale: List[(Prototype[Double], (String, String))], genome: List[Double], context: Context): List[Variable[Double]] =
    if (scale.isEmpty || genome.isEmpty) List.empty
    else {
      val (p, (vMin, vMax)) = scale.head
      val dMin = VariableExpansion(context, vMin).toDouble
      val dMax = VariableExpansion(context, vMax).toDouble
      val scaledV = Variable(p, genome.head.scale(dMin, dMax))
      scaledV :: scaled(scale.tail, genome.tail, context + scaledV)
    }

  def apply[T <: GAGenome](
    name: String,
    genome: Prototype[T],
    scale: (Prototype[Double], (String, String))*)(implicit plugins: PluginSet) =
    new TaskBuilder { builder ⇒
      scale foreach { case (p, _) ⇒ this.addOutput(p) }
      addInput(genome)
      addOutput(genome)

      def toTask = new ScalingGAGenomeTask[T](name, genome, scale: _*) with Built
    }

}

sealed abstract class ScalingGAGenomeTask[T <: GAGenome](
    val name: String,
    genome: Prototype[T],
    scale: (Prototype[Double], (String, String))*) extends Task {

  override def process(context: Context) =
    context ++ ScalingGAGenomeTask.scaled(scale.toList, context(genome).values.toList, context)

}
