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
import java.math.MathContext
import org.openmole.core.implementation.tools.VariableExpansion._
import org.openmole.misc.math.BigDecimalOperations._
import java.math.BigDecimal

class BigDecimalLogarithmRange(val min: String, val max: String, val nbStep: String) extends IFiniteDomain[BigDecimal] with IWithRange[BigDecimal] {
  
  override def range(global: IContext, context: IContext): BigDecimal = {max(global, context).subtract(min(global, context))}
    
  override def computeValues(global: IContext, context: IContext): Iterable[BigDecimal] = {
    val mi = log(min(global, context))
    val ma = log(max(global, context))
    val nbst = nbStep(global, context)
    val st = ma.subtract(mi).abs.divide(nbst)

    var cur = mi
        
    for (i <- 0 to nbst.intValue) yield {
      val ret = cur
      cur = cur.add(st)
      ret
    }      
  }
  
  def nbStep(global: IContext, context: IContext): BigDecimal = new BigDecimal(expandData(global, context, nbStep), MathContext.DECIMAL128)
  def min(global: IContext, context: IContext): BigDecimal = new BigDecimal(expandData(global, context, min), MathContext.DECIMAL128)
  def max(global: IContext, context: IContext): BigDecimal = new BigDecimal(expandData(global, context, max), MathContext.DECIMAL128)
}
