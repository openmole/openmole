/*
 * Copyright (C) 2010 reuillon
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

package org.openmole.commons.tools.service

object ManifestUtil {
  import java.lang.reflect.{ Type => JType, Array => _, _ }
  import scala.reflect.Manifest.{ classType, intersectionType, arrayType, wildcardType }

  def intersect(tps: JType*): Manifest[_] = intersectionType(tps map javaType: _*)
  
  def javaType(tp: JType): Manifest[_] = tp match {
    case x: Class[_]            => classType(x)
    case x: ParameterizedType   =>
      val owner = x.getOwnerType
      val raw   = x.getRawType() match { case clazz: Class[_] => clazz }
      val targs = x.getActualTypeArguments() map javaType

      (owner == null, targs.isEmpty) match {
        case (true, true)   => javaType(raw)
        case (true, false)  => classType(raw, targs.head, targs.tail: _*)
        case (false, _)     => classType(javaType(owner), raw, targs: _*)
      }
    case x: GenericArrayType    => arrayType(javaType(x.getGenericComponentType))
    case x: WildcardType        => wildcardType(intersect(x.getLowerBounds: _*), intersect(x.getUpperBounds: _*))
    case x: TypeVariable[_]     => intersect(x.getBounds(): _*)
  }
 
  
}
