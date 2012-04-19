/*
 * Copyright (C) 2012 reuillon
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

import fr.iscpif.mgo.ga.GAGenome
import fr.iscpif.mgo.tools.Scaling._
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.task.Task
import org.openmole.core.implementation.task.TaskBuilder
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.core.model.sampling._
import org.openmole.core.model.domain._

import scala.collection.mutable.ListBuffer

object ScalingGenomeTask {
  
  def apply[T <: GAGenome](
    name: String, 
    genome: IPrototype[T], 
    scale: (IPrototype[Double], (Double, Double))*)(implicit plugins: IPluginSet) = 
    new TaskBuilder { builder => 
      def toTask = new ScalingGenomeTask[T](name, genome, scale: _*) {
        val inputs = builder.inputs + genome
        val outputs = builder.outputs + genome
        val parameters = builder.parameters
      }
    }
  
}

sealed abstract class ScalingGenomeTask[T <: GAGenome] (
  val name: String,
  genome: IPrototype[T],
  scale: (IPrototype[Double], (Double, Double))*
) 
(implicit val plugins: IPluginSet) extends Task {

  override def process(context: IContext) = {
    context ++ 
    (scale zip context.valueOrException(genome).values).map {
      case(s, g) => 
        val (p, (min, max)) = s
        new Variable(p, g.scale(min, max))
    }
  }
}
