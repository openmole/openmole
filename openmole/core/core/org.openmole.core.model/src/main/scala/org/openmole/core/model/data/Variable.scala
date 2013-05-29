/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.model.data

import org.openmole.misc.tools.io.Prettifier._

object Variable {
  def apply[T](p: Prototype[T], v: T) = new Variable[T] {
    val prototype = p
    val value = v
  }

  def unsecure[T](p: Prototype[T], v: Any) = new Variable[T] {
    val prototype = p
    val value = v.asInstanceOf[T]
  }

}

trait Variable[T] {
  def prototype: Prototype[T]
  def value: T

  override def toString: String = prettified(Int.MaxValue)

  def prettified(snipArray: Int) = prototype.name + "=" + (if (value != null) value.prettify(snipArray) else "null")
}

