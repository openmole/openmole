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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.domain.range

import org.openmole.core.model.data.IContext

abstract class IntegralRange[T](val min: String, val max: String, val step: String)(implicit integral: Integral[T]) extends IRange[T] {
  
  import integral._
  
  override def computeValues(context: IContext): Iterable[T] = {
    val mi = min(context)
    val ma = max(context)
    val s =  step(context)
    
    val size = (ma - mi).abs / s
    
    var cur = mi
    (for(i <- 0 to size.toInt) yield {val ret = cur; cur += s; ret})   
  }
    
  override def center(context: IContext): T = {
    val mi = min(context);
    mi + ((max(context) - mi) / fromInt(2))
  }

  override def range(context: IContext): T = {
    max(context) - min(context)
  }
}
