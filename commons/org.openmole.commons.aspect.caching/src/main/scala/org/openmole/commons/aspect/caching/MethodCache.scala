/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.commons.aspect.caching

import scala.collection.mutable.HashMap
import scala.collection.mutable.SynchronizedMap
import scala.collection.mutable.WeakHashMap

class MethodCache {
    
    val cache = new WeakHashMap[Object, HashMap[String, Object]] with SynchronizedMap[Object, HashMap[String, Object]]

    def putCachedMethodResult(obj: Object, method: String, result: Object) = {
        val methodMap = cache.getOrElseUpdate(obj, new HashMap[String, Object] with SynchronizedMap[String, Object])
        methodMap.put(method, result)
    }

    def cachedMethodResult(obj: Object, method: String): Object = {
        cache.get(obj) match {
          case None => return null
          case Some(methodMap) => methodMap.getOrElse(method, null)
        }

    }


    def clear(obj: Object) = cache.remove(obj)

    def size = cache.size
    
}
