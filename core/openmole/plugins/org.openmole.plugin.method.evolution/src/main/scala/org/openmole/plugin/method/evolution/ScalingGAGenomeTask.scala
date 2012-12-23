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

  def min(p: Prototype[Double]) =
    Prototype(p.name + "Min")(p.`type`)

  def max(p: Prototype[Double]) =
    Prototype(p.name + "Max")(p.`type`)

  def expand(scale: List[(Prototype[Double], (String, String))], context: Context): List[(Prototype[Double], (Double, Double))] =
    if (scale.isEmpty) List.empty
    else {
      val (p, (vMin, vMax)) = scale.head
      val dVMin = VariableExpansion(context, vMin).toDouble
      val varMin = Variable(min(p), dVMin)
      val dVMax = VariableExpansion(context + varMin, vMax).toDouble
      val varMax = Variable(max(p), dVMax)
      (p, dVMin -> dVMax) :: expand(scale.tail, context + varMin + varMax)
    }

  def apply[T <: GAGenome](
    name: String,
    genome: Prototype[T],
    scale: (Prototype[Double], (String, String))*)(implicit plugins: PluginSet) =
    new TaskBuilder { builder ⇒
      scale foreach { case (p, _) ⇒ this.addOutput(p) }
      addInput(genome)
      addOutput(genome)

      def toTask = new ScalingGAGenomeTask[T](name, genome, scale: _*) {
        val inputs = builder.inputs
        val outputs = builder.outputs
        val parameters = builder.parameters
      }
    }

}

sealed abstract class ScalingGAGenomeTask[T <: GAGenome](
    val name: String,
    genome: Prototype[T],
    scale: (Prototype[Double], (String, String))*)(implicit val plugins: PluginSet) extends Task {

  override def process(context: Context) = {
    context ++
      (ScalingGAGenomeTask.expand(scale.toList, context) zip context(genome).values).map {
        case (s, g) ⇒
          val (p, (min, max)) = s
          Variable(p, g.scale(min, max))
      }
  }
}
