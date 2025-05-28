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

import org.openmole.tool.collection.DisposableRepository

import java.util.concurrent.locks.ReentrantLock

object LockRepository:
  def apply[T]() = new LockRepository[T]()

class LockRepository[T]:
  val locks = DisposableRepository[T, ReentrantLock](_ => new ReentrantLock())

  def locked[A](obj: T)(op: => A) =
    locks.borrow(obj): lock =>
      lock.lock()
      try op
      finally lock.unlock()


