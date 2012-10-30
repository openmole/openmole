/*
 * Copyright (C) 2012 reuillon
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

package org.openmole.core.batch.replication

import collection.mutable.{ HashSet, SynchronizedSet }
import com.db4o.ObjectContainer
import org.openmole.misc.tools.service.LockRepository

trait LockCache {
  val lockRepository = new LockRepository[String]
  val lockCache = new HashSet[String] with SynchronizedSet[String]

  def lock(k: String)
  def unlock(k: String)

  def withLock[A](k: String)(op: ⇒ A): A = lockRepository.withLock(k) {
    try {
      if (!lockCache.contains(k)) {
        lock(k)
        lockCache += k
      }
      op
    } finally {
      if (lockRepository.nbLocked(k) == 1) {
        unlock(k)
        lockCache -= k
      }
    }
  }

}
