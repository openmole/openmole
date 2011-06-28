/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.misc.tools.service

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import scala.collection.mutable.HashMap

class LockRepository[T] {

  val locks = new HashMap[T, (Lock, AtomicInteger)]

  def lock(obj: T) = synchronized {
    val lock = locks.getOrElseUpdate(obj,(new ReentrantLock, new AtomicInteger(0)))
    lock._2.incrementAndGet
    lock._1
  }.lock
    
  def unlock(obj: T) = synchronized {
    locks.get(obj) match {
      case Some(lock) => 
        val value = lock._2.decrementAndGet
        if (value <= 0) locks.remove(obj)
        lock._1
      case None => throw new IllegalArgumentException("Unlocking an object that has not been locked.")
    }
  }.unlock
  
}
