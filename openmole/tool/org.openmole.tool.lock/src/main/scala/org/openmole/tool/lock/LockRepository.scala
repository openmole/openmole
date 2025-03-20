/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.tool.lock

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.{ Lock, ReentrantLock }

import scala.collection.mutable

object LockRepository:
  def apply[T]() = new LockRepository[T]()

class LockRepository[T]:

  val locks = new mutable.HashMap[T, (ReentrantLock, AtomicInteger)]

  def nbLocked(k: T) = locks.synchronized(locks.get(k).map { (_, users) => users.get }.getOrElse(0))

  private def getLock(obj: T) = locks.synchronized:
    val (lock, users) = locks.getOrElseUpdate(obj, (new ReentrantLock, new AtomicInteger(0)))
    users.incrementAndGet
    lock

  private def cleanLock(obj: T) = locks.synchronized:
    locks.get(obj) match
      case Some((lock, users)) =>
        val value = users.decrementAndGet
        if (value <= 0) locks.remove(obj)
        lock
      case None => throw new IllegalArgumentException("Unlocking an object that has not been locked.")


  def withLock[A](obj: T)(op: => A) =
    val lock = getLock(obj)
    lock.lock()
    try op
    finally
      try cleanLock(obj)
      finally lock.unlock()


