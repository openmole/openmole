/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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

package org.openmole.plugin.domain.range

import org.openmole.core.model.data.IContext
import org.openmole.core.model.domain.IFiniteDomain
import org.openmole.core.model.domain.IWithRange
import org.openmole.core.implementation.tools.VariableExpansion._
import java.lang.Double

class LogarithmDoubleRange(val min: String, val max: String, val nbStep: String) extends IFiniteDomain[Double] with IWithRange[Double] {
  
  override def range(global: IContext, context: IContext): Double = {max(global, context).doubleValue - min(global, context).doubleValue}
    
  override def computeValues(global: IContext, context: IContext): Iterable[Double] = {
    val mi = java.lang.Math.log(min(global, context).doubleValue)
     val ma = java.lang.Math.log(max(global, context).doubleValue)
     val nbst = nbStep(global, context).intValue
     val st = (math.abs(ma - mi) / nbst)

     var cur = mi
        
     for (i <- 0 to nbst) yield {
       val ret = cur; 
       cur += st
       new Double(ret)
     }      
  }
  
  def nbStep(global: IContext, context: IContext): Double = expandData(global, context, nbStep).toDouble
  def min(global: IContext, context: IContext): Double = expandData(global, context, min).toDouble
  def max(global: IContext, context: IContext): Double = expandData(global, context, max).toDouble

}
