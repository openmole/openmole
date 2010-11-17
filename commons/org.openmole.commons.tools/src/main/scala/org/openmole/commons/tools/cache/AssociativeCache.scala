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

import java.util.WeakHashMap
import java.util.Collections
import java.util.Map
import org.apache.commons.collections15.map.ReferenceMap
import org.openmole.commons.tools.service.LockRepository
import org.apache.commons.collections15.map.AbstractReferenceMap

object AssociativeCache {
  val WEAK = AbstractReferenceMap.WEAK
  val SOFT = AbstractReferenceMap.SOFT
  val HARD = AbstractReferenceMap.HARD
}

class AssociativeCache[K, T](keyRef: Int, valRef: Int) {

  val cacheMaps = new WeakHashMap[Object, Map[K, T]]
  val lockRepository = new LockRepository[K]

  def invalidateCache(cacheAssociation: Object, key: K) = {
    val cache = cacheMaps.synchronized {
      cacheMaps.get(cacheAssociation) 
    }
        
    if (cache != null) {
      lockRepository.lock(key)
      try {
        cache.remove(key)
      } finally {
        lockRepository.unlock(key)
      }
    } 


  }

  def cached(cacheAssociation: Object, key: K): Option[T] = {
    val cache = cacheMaps.synchronized {
      cacheMaps.get(cacheAssociation)
    }
    if (cache == null) None
    else {
      val ret = cache.get(key) 
      if(ret == null) None
      else Some(ret)
    }
  }

  def cache(cacheAssociation: Object, key: K, cachable: => T): T = {
    val cache = cacheMap(cacheAssociation)
    var ret = cache.get(key)

    if(ret != null) return ret
    lockRepository.lock(key)
    try {
      if (ret == null) {
        ret = cachable
        cache.put(key, ret)
      }
    } finally {
      lockRepository.unlock(key)
    }

    return ret
  }

  def cacheMap(cacheAssociation: Object): Map[K, T] = {
    cacheMaps.synchronized  {
      var ret = cacheMaps.get(cacheAssociation);
      if (ret == null) {
        ret = Collections.synchronizedMap(new ReferenceMap(keyRef, valRef));
        cacheMaps.put(cacheAssociation, ret);
      }
      ret
    }
  }
}
