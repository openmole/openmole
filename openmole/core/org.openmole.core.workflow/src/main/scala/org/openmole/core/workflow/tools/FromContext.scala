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

package org.openmole.core.workflow.tools

import org.openmole.core.tools.io.FromString
import org.openmole.core.tools.script.GroovyProxyPool
import org.openmole.core.workflow.data._
import org.openmole.core.tools.io._
import org.openmole.core.tools.script._

object FromContext {

  implicit def fromTToContext[T](t: T) = FromContext[T](t)

  implicit def fromStringToContext[T](code: String)(implicit fromString: FromString[T]) =
    new FromContext[T] {
      @transient lazy val proxy = GroovyProxyPool(code)
      override def from(context: ⇒ Context): T = fromString.from(proxy(context.toBinding).toString)
    }

  def apply[T](t: T) =
    new FromContext[T] {
      def from(context: ⇒ Context): T = t
    }

}

trait FromContext[T] {
  def from(context: ⇒ Context): T
}

object ExpandedString {

  implicit def fromStringToExpandedString(s: String) = ExpandedString(s)
  implicit def fromStringToExpandedStringOption(s: String) = Some[ExpandedString](s)
  implicit def fromTraversableOfStringToTraversableOfExpandedString[T <: Traversable[String]](t: T) = t.map(ExpandedString(_))

  def apply(s: String) =
    new ExpandedString {
      override def string = s
    }
}

trait ExpandedString <: FromContext[String] {
  def +(s: ExpandedString): ExpandedString = string + s.string
  def string: String
  def from(context: ⇒ Context) = VariableExpansion.apply(context, string)
}

