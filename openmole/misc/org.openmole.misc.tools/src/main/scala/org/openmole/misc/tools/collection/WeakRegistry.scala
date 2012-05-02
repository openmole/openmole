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

import scala.collection.mutable.SynchronizedMap
import scala.collection.mutable.WeakHashMap

class WeakRegistry[K, V] {

  val registry = new WeakHashMap[K, V] with SynchronizedMap[K, V]

  def isRegistred(key: K): Boolean = registry.contains(key)

  def +=(v: (K, V)) = { registry += v }

  def remove(key: K): Option[V] = { registry.remove(key) }

  def apply(key: K): Option[V] = registry.get(key)

}
