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

package org.openmole.core.implementation.data

import org.openmole.core.model.data.IVariable
import org.openmole.core.model.data.IPrototype

object Variable {
  def apply[C,T <: C](prototype: IPrototype[C], value: T): Variable[C] = new Variable[C](prototype, value)
}

class Variable[C](val prototype: IPrototype[C], val value: C) extends IVariable[C] {

  def this(name: String, `type`: Class[C] , value: C) = this(new Prototype[C](name, `type`), value)

  def this(name: String, value: C) = this(name, value.asInstanceOf[AnyRef].getClass.asInstanceOf[Class[C]], value)
  
  def this(prototype: IPrototype[C]) = this(prototype, null.asInstanceOf[C])

  def this(name: String, `type`: Class[C]) = this(new Prototype[C](name, `type`))
  
  override def toString: String = {
    prototype.name + '=' + (if(value != null) value.toString else "null")
  }
}
