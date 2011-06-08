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

package org.openmole.core.implementation.data

import org.openmole.core.model.data.IPrototype
import org.openmole.misc.tools.obj.ClassUtils._
import org.openmole.misc.tools.obj.Id
import scala.reflect.Manifest

object Prototype {
  def toArray[T](prototype: IPrototype[T]): IPrototype[Array[T]] = {
    new Prototype(prototype.name, prototype.`type`.arrayManifest).asInstanceOf[IPrototype[Array[T]]]
  }
}

class Prototype[T](val name: String, val `type`: Manifest[T]) extends IPrototype[T] with Id {

  def this(name: String, `type`: Class[T]) = this(name, `type`.equivalence.toManifest)
  
  override def isAssignableFrom(p: IPrototype[_]): Boolean = `type` <:< p.`type`
  
  override def accepts(obj: Any): Boolean = {
    obj == null || `type`.isAssignableFrom(manifest(clazzOf(obj)))
  }
  
  override def id = (name, `type`)
  override def toString: String = '(' + `type`.toString + ')' + name
}
