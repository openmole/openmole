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

import gears.async.*
import gears.async.default.given
import org.openmole.tool.collection.PriorityQueue

class PrioritySemaphore(initialPermits: Int):
  private var permits = initialPermits
  private val waitQueue = PriorityQueue[Future.Promise[Unit]]()

  def acquire(priority: Int): Unit =
    val promise: Option[Future.Promise[Unit]] =
      synchronized:
        if permits > 0
        then
          permits -= 1
          None
        else
          val promise = Future.Promise[Unit]()
          waitQueue.enqueue(promise, priority)
          Some(promise)

    Async.blocking:
      promise.foreach:p =>
        p.await

  def release(): Unit =
    synchronized:
      if !waitQueue.isEmpty
      then
        val nextPromise = waitQueue.dequeue()
        nextPromise.complete(util.Success(()))
      else permits += 1



