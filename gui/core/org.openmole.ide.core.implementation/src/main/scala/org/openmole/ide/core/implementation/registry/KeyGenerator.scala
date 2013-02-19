/*
 * Copyright (C) 2012 mathieu
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

package org.openmole.ide.core.implementation.registry

import org.openmole.core.model.data._
import org.openmole.misc.tools.obj.ClassUtils._
import scala.annotation.tailrec

object KeyGenerator {
  @tailrec def stripArrays(m: Manifest[_], dim: Int = 0): (Manifest[_], Int) = {
    if (m.erasure.isArray) stripArrays(m.erasure.fromArray.toManifest, dim + 1)
    else (m, dim)
  }

  def apply(proto: Prototype[_]): (PrototypeKey, Int) = {
    val (manifest, dim) = stripArrays(proto.`type`)
    (new PrototypeKey(proto.name, manifest.runtimeClass, dim), dim)
  }

  def apply(entityClass: Class[_]): DefaultKey = new DefaultKey(entityClass)

  def apply(entityName: String): NameKey = new NameKey(entityName)
}
