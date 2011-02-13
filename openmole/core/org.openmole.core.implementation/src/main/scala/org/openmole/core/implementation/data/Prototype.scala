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

import org.openmole.core.model.data.IPrototype

object Prototype {
  def toArray[T](prototype: IPrototype[T]): IPrototype[Array[T]] = {
    val arrayClass = java.lang.reflect.Array.newInstance(prototype.`type`, 0).getClass
    new Prototype(prototype.name, arrayClass).asInstanceOf[IPrototype[Array[T]]]
  }
}

case class Prototype[T](val name: String, val `type`: Class[T]) extends IPrototype[T] {

  override def isAssignableFrom(p: IPrototype[_]): Boolean = `type`.isAssignableFrom(p.`type`)

  override def toString: String = '(' + `type`.getName + ')' + name

}
