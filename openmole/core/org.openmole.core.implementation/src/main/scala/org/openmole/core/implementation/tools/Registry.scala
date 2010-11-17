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

package org.openmole.core.implementation.tools

import scala.collection.mutable.HashMap
import scala.collection.mutable.SynchronizedMap

class Registry [K, V] extends IRegistry[K, V] {

    val registry = new HashMap[K, V] with SynchronizedMap[K,V]

    override def isRegistred(key: K): Boolean = registry.contains(key)
    
    override def +=(key: K, value: V) = registry.put(key, value)
    
    override def apply(key: K): Option[V] = registry.get(key)
   
    override def remove(key: K): Option[V] = registry.remove(key)
}
