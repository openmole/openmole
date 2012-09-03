/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.plugin.domain.range

import org.openmole.core.model.data._
import org.openmole.core.model.domain._
import org.openmole.core.implementation.tools._
import math._

sealed class DoubleLogarithmRange(val min: String, val max: String, val nbStep: String) extends IDomain[Double] with IFinite[Double] with IBounded[Double] {

  def this(min: Double, max: Double, nbStep: Int) = this(min.toString, max.toString, nbStep.toString)

  override def computeValues(context: Context): Iterable[Double] = {
    val mi = log(min(context).doubleValue)
    val ma = log(max(context).doubleValue)
    val nbst = nbStep(context).intValue - 1
    val st = if (nbst > 0) (math.abs(ma - mi) / nbst)
    else 0
    var cur = mi

    for (i ← 0 to nbst) yield {
      val ret = cur
      cur += st
      exp(ret)
    }
  }

  def nbStep(context: Context): Double = VariableExpansion(context, nbStep).toDouble
  def min(context: Context): Double = VariableExpansion(context, min).toDouble
  def max(context: Context): Double = VariableExpansion(context, max).toDouble

}
