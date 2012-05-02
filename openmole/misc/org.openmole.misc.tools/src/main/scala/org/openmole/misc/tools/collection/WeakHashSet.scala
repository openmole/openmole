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

package org.openmole.misc.tools.collection

import scala.collection.mutable.Set
import scala.collection.mutable.WeakHashMap

//TODO?reimplemented a true weak hashset
class WeakHashSet[A] extends Set[A] {

  val _values = new WeakHashMap[A, AnyRef]

  override def contains(key: A): Boolean = _values.contains(key)
  override def iterator: Iterator[A] = _values.keysIterator

  override def +=(elt: A): this.type = { _values(elt) = None; this }
  override def -=(elt: A): this.type = { _values -= elt; this }

  override def empty: this.type = { _values.empty; this }
  override def size = _values.size
}
