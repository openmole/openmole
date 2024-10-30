package org.openmole.tool.lock

/*
 * Copyright (C) 2024 Romain Reuillon
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

import org.openmole.tool.collection.PriorityQueue

import scala.jdk.CollectionConverters.*
import java.util.concurrent.Semaphore



class PrioritySemaphore(initialPermits: Int):
  private var permits = initialPermits
  val locks = PriorityQueue[Semaphore]()

  def acquire(priority: Int): Unit =
    val semaphore =
      synchronized:
        if permits > 0
        then
          permits -= 1
          None
        else
          val lock = new Semaphore(0)
          locks.enqueue(lock, priority)
          Some(lock)

    semaphore.foreach(_.acquire())

  def release(): Unit =
    synchronized:
      locks.dequeue() match
        case None => permits += 1
        case Some(l) => l.release()


