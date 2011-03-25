/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.misc.tools.obj

trait Id {
  def id: AnyRef
  
  override def hashCode = id.hashCode

  override def equals(other: Any): Boolean = {
    if(other == null) false
    else if(!classOf[Id].isAssignableFrom(other.asInstanceOf[AnyRef].getClass)) false
    else id.equals(other.asInstanceOf[Id].id)
  }
  
  override def toString = id.toString
}
