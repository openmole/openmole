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

package org.openmole.tool.cache

import scala.collection.mutable.{ HashMap, WeakHashMap }

class AssociativeCache[K, T] {

  val cacheMaps = new WeakHashMap[Object, HashMap[K, T]]

  def invalidateCache(cacheAssociation: Object, key: K) = cacheMaps.synchronized {
    for { cache ← cacheMaps.get(cacheAssociation) } {
      cache -= key
      if (cache.isEmpty) cacheMaps.remove(cacheAssociation)
    }
  }

  def cached(cacheAssociation: Object, key: K): Option[T] = cacheMaps.synchronized {
    cacheMaps.get(cacheAssociation) match {
      case None      => None
      case Some(map) => map.synchronized { map.get(key) }
    }
  }

  def cache(cacheAssociation: Object, key: K, preCompute: Boolean = true)(cacheable: K => T): T = {
    def cache = {
      val computedCache = if (preCompute) Some(cacheable(key)) else None
      cacheMaps.synchronized {
        def cacheMap(cacheAssociation: Object): HashMap[K, T] =
          cacheMaps.getOrElseUpdate(cacheAssociation, new HashMap[K, T])

        val cache = cacheMap(cacheAssociation)
        cache.getOrElseUpdate(key, computedCache.getOrElse(cacheable(key)))
      }
    }

    cached(cacheAssociation, key).getOrElse(cache)
  }

}
