/*
 *  Copyright (C) 2010 leclaire
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.plugin.task.stat


import org.openmole.misc.math.Stat
import org.openmole.misc.tools.io.Prettifier._
import org.openmole.core.implementation.data.Context
import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.task.Task
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.data.IContext


class MedianAbsoluteDeviationTask(name: String) extends Task(name) {

  var deviations: List[(IPrototype[Array[Double]], IPrototype[Double])] = Nil

  def deviation(serie: IPrototype[Array[Double]], deviation: IPrototype[Double]) = {
    addInput(serie)
    addOutput(deviation)
    deviations ::= (serie, deviation)
  }
  
  override def process(context: IContext) =    
    Context(
      deviations.map {
        case(serie, deviation) => 
          new Variable(deviation, Stat.medianAbsoluteDeviation(context.valueOrException(serie)))
      }
    )
   
}
