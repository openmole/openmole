/*
 * Copyright (C) 2014 Romain Reuillon
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

package org.openmole.core.implementation.tools

import org.openmole.core.model.data._
import org.openmole.misc.tools.io.FromString
import org.openmole.misc.tools.script.GroovyProxyPool

object FromContext {

  implicit def fromStringToContext[T](code: String)(implicit fromString: FromString[T]) =
    new FromContext[T] {
      @transient lazy val proxy = GroovyProxyPool(code)
      override def from(context: Context): T = fromString.from(proxy(context).toString)
    }

  implicit def fromTToContext[T](t: T) = FromContext[T](t)

  implicit def fromStringToExpandedString(s: String) =
    new ExpandedString {
      override def string = s
    }

  def apply[T](t: T) =
    new FromContext[T] {
      def from(context: Context): T = t
    }

}

trait FromContext[T] {
  def from(context: Context): T
}

trait ExpandedString <: FromContext[String] {
  def string: String
  def from(context: Context) = VariableExpansion.apply(context, string)
}

