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
import java.math.RoundingMode
import java.util.logging.Logger
import org.openmole.core.implementation.tools.VariableExpansion._
import org.openmole.misc.math.BigDecimalOperations._
import java.math.BigDecimal

object BigDecimalLogarithmRange {
  val scale = 128
}


class BigDecimalLogarithmRange(val min: String, val max: String, val nbStep: String) extends IFiniteDomain[BigDecimal] with IWithRange[BigDecimal] {
  import BigDecimalLogarithmRange._
  
  override def range(global: IContext, context: IContext): BigDecimal = {max(global, context).subtract(min(global, context))}
    
  override def computeValues(global: IContext, context: IContext): Iterable[BigDecimal] = {
    val minValue = min(global, context)
    val mi = ln(minValue, scale)
    
    val maxValue = max(global, context)
    val ma = ln(maxValue, scale)

    val retScale = math.max(minValue.scale, maxValue.scale)
    
    val nbst = nbStep(global, context).intValue - 1
    val step = ma.subtract(mi).abs.divide(new BigDecimal(nbst), BigDecimal.ROUND_HALF_UP)

    var cur = mi
    val mc = new MathContext(retScale, RoundingMode.HALF_UP)
    //Logger.getLogger(getClass.getName).info(" " + minValue + " " + retScale)
    
    for (i <- 0 to nbst) yield {
      val ret = cur
      cur = cur.add(step)
      exp(ret, scale).setScale(retScale, RoundingMode.HALF_UP).round(mc) 
      //val expRet = exp(ret, scale)
      //expRet.setScale(retScale, BigDecimal.ROUND_HALF_UP)
      //expRet
    }      
  }
  
  def nbStep(global: IContext, context: IContext): BigDecimal = new BigDecimal(expandData(global, context, nbStep))
  def min(global: IContext, context: IContext): BigDecimal = new BigDecimal(expandData(global, context, min))
  def max(global: IContext, context: IContext): BigDecimal = new BigDecimal(expandData(global, context, max))
}
