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
import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.task.Task
import org.openmole.core.implementation.task.TaskBuilder
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.task.IPluginSet

object ScalingGenomeTask {
  
  def apply[T <: GAGenome](name: String, genome: IPrototype[T])(implicit plugins: IPluginSet) = 
    new TaskBuilder { builder => 
      var _scale: List[(IPrototype[Double], Double, Double)] = List.empty
      
      def scale = new {
        def +=(p: IPrototype[Double], min: Double, max: Double) = {
          _scale ::= ((p, min, max))
          outputs += p
          builder
        }
      }
      
      def toTask = new ScalingGenomeTask[T](name, genome) {
        val inputs = builder.inputs + genome
        val outputs = builder.outputs + genome
        val parameters = builder.parameters
        val scale = _scale.reverse
      }
    }
  
}

sealed abstract class ScalingGenomeTask[T <: GAGenome]
(val name: String,
 genome: IPrototype[T]) 
(implicit val plugins: IPluginSet) extends Task {

  def scale: List[(IPrototype[Double], Double, Double)]
  
  override def process(context: IContext) = {
    context ++ 
      scale.reverse.zip(context.valueOrException(genome).values).map {
        case((p, min, max), g) => new Variable(p, g.scale(min, max))
      }
  }
}
