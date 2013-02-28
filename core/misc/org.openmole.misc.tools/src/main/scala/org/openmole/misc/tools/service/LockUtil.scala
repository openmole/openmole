/*
 * Copyright (C) 16/12/12 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.misc.tools.service

import java.util.concurrent.locks.Lock
import java.util.concurrent.Semaphore

object LockUtil {

  implicit class LockDecorator(lock: Lock) {
    def apply[T](block: ⇒ T): T = {
      lock.lock()
      try {
        block
      } finally lock.unlock()
    }
  }

  implicit class SemaphoreDecorator(s: Semaphore) {
    def apply[T](block: ⇒ T): T = {
      s.acquire()
      try {
        block
      } finally s.release()
    }
  }

}
