/*
 * Copyright (C) 2015 Romain Reuillon
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
package org.openmole.tool

import java.util.UUID
import java.util.concurrent.Semaphore
import java.util.concurrent.locks._

package object lock {

  implicit class LockDecorator(lock: Lock):
    def apply[T](block: ⇒ T): T =
      lock.lock()
      try
        block
      finally lock.unlock()

  implicit class SemaphoreDecorator(s: Semaphore):
    def apply[T](block: ⇒ T): T =
      s.acquire()
      try block
      finally s.release()

    def acquireAndRelease() =
      s.acquire()
      s.release()

  implicit class ReadWriteLockDecorator(l: ReadWriteLock):

    def read[T](t: ⇒ T) =
      l.readLock.lock
      try t
      finally l.readLock.unlock

    def write[T](t: ⇒ T) =
      l.writeLock.lock
      try t
      finally l.writeLock.unlock


  case class LockKey(id: UUID = java.util.UUID.randomUUID())
}
