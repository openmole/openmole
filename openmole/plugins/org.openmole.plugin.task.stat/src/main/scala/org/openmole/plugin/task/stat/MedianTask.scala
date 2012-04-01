/*
 * Copyright (C) 2011 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.task.stat

import org.openmole.core.implementation.data.Context
import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.task.Task
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.data.IContext
import org.openmole.misc.math.Stat

class MedianTask(name: String) extends Task(name) {
  
  var medians: List[(IPrototype[Array[Double]], IPrototype[Double])] = Nil

  def median(serie: IPrototype[Array[Double]], median: IPrototype[Double]) = {
    addInput(serie)
    addOutput(median)
    medians ::= (serie, median)
  }
  
  override def process(context: IContext) = 
    Context(
      medians.map{
        case(serie, median) => new Variable(median, Stat.median(context.valueOrException(serie)))
      }
    )
  

}
