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

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import scala.collection.mutable.HashMap
import scala.collection.mutable.SynchronizedMap
import scala.collection.mutable.WeakHashMap

class WeakLockRepository {
  val locks  = new WeakHashMap[Object, HashMap[String,Lock]] with SynchronizedMap[Object, HashMap[String,Lock]]
	
  def lockFor(obj: Object, method: String): Lock = {
    val lockMap = locks.getOrElseUpdate(obj, new HashMap[String, Lock] with SynchronizedMap[String, Lock])
    lockMap.getOrElseUpdate(method, new ReentrantLock)
  }

}
