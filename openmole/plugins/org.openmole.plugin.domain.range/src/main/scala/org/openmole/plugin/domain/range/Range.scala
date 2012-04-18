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

package org.openmole.plugin.domain.range


import org.openmole.core.model.data.IContext
import org.openmole.core.implementation.tools.VariableExpansion._

sealed class Range[T](val min: String, val max: String, val step: String = "1")(implicit integral: Integral[T], fs: FromString[T]) extends IRange[T] {
  
  import integral._
  import fs._
  
  override def computeValues(context: IContext): Iterable[T] = {
    val mi = min(context)
    val ma = max(context)
    val s =  step(context)
    
    val size = (ma - mi).abs / s
    
    (for(i <- 0 to size.toInt) yield {mi + fromInt(i) * s})
  }
    
  override def center(context: IContext): T = {
    val mi = min(context);
    mi + ((max(context) - mi) / fromInt(2))
  }

  override def step(context: IContext): T = fromString(expandData(context, step))
  override def max(context: IContext): T = fromString(expandData(context, max))
  override def min(context: IContext): T = fromString(expandData(context, min))
  
}

