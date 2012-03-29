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

package org.openmole.plugin.evolution

import fr.iscpif.mgo.ga.GAGenome
import fr.iscpif.mgo.tools.Scaling._
import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.task.Task
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype

class ScalingGenomeTask[T <: GAGenome](name: String, genome: IPrototype[T]) extends Task(name) {

  addInput(genome)
  addOutput(genome)
  
  var scaled: List[(IPrototype[Double], Double, Double)] = Nil
  
  def scale(p: IPrototype[Double], min: Double, max: Double) = {
    scaled ::= ((p, min, max))
    addOutput(p)
  }
  
  override def process(context: IContext) = {
    context ++ 
      scaled.reverse.zip(context.valueOrException(genome).values).map {
        case((p, min, max), g) => new Variable(p, g.scale(min, max))
      }
  }
}
