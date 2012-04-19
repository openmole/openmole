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

package org.openmole.core.implementation.data

import org.openmole.core.model.data.IVariable
import org.openmole.core.model.data.IPrototype
import org.openmole.misc.tools.io.Prettifier._

class LazyVariable[C](val prototype: IPrototype[C], valueFunction: => C) extends IVariable[C] {
  
  lazy val value: C = valueFunction
  
  override def toString: String = 
    prototype.name + "=" + (if(value != null) value.prettify else "null")

}
