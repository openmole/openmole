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

package org.openmole.commons.tools.cache

import scala.collection.mutable.HashMap
import scala.collection.mutable.SynchronizedMap
import scala.collection.mutable.WeakHashMap

class AssociativeCache[K, T] {

  val cacheMaps = new WeakHashMap[Object, HashMap[K, T]] with SynchronizedMap[Object, HashMap[K, T]]

  def invalidateCache(cacheAssociation: Object, key: K) = {
    val cache = cacheMaps.getOrElse(cacheAssociation, null)
    if (cache != null) cache -= key
  }

  def cached(cacheAssociation: Object, key: K): Option[T] = {
    cacheMaps.get(cacheAssociation) match {
      case None => None
      case Some(map) => map.get(key)
    }
  }

  def cache(cacheAssociation: Object, key: K, cachable : => T): T = {    
    val cache = cacheMap(cacheAssociation)
    return cache.getOrElseUpdate(key, cachable)
  }

  def cacheMap(cacheAssociation: Object): HashMap[K, T] = {
    cacheMaps.getOrElseUpdate(cacheAssociation, new HashMap[K,T] with SynchronizedMap[K,T])
  }
}
