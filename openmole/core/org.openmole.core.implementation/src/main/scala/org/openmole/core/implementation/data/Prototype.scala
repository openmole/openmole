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
import java.lang.Iterable

object Prototype {
  def toArray[T](prototype: IPrototype[T]): IPrototype[Iterable[_ >: T]] = new Prototype[Iterable[_ >: T]](prototype.name, classOf[Iterable[_ >: T]])
}

class Prototype[T](val name: String, val `type`: Class[T]) extends IPrototype[T] {

/*  def this(prototype: IPrototype[T], name: String) = {
    this(name, prototype.`type`)
  }

  def this(prototype: IPrototype[T], `type`: Class[T]) = {
    this(prototype.name, `type`)
  }*/

  override def isAssignableFrom(p: IPrototype[_]): Boolean = {
    `type`.isAssignableFrom(p.`type`)
  }

  override def toString: String = {
    '(' + `type`.getName + ')' + name
  }

}
