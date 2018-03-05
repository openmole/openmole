/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.tool.collection

import java.util.concurrent.Semaphore
import java.util.TreeMap
import collection.JavaConverters._
import java.util.Stack

object PriorityQueue {
  def apply[T]() = new PriorityQueue[T]
}

class PriorityQueue[T] {

  private val inQueue = new Semaphore(0)

  val queues = (new TreeMap[Int, Stack[T]]).asScala

  def size: Int = synchronized { queues.map { case (_, q) ⇒ q.size }.sum }

  def enqueue(e: T, priority: Int) = {
    synchronized {
      queues.get(priority) match {
        case Some(queue) ⇒ queue.push(e)
        case None ⇒
          val q = new Stack[T]
          q.push(e)
          queues.put(priority, q)
      }
    }
    inQueue.release
  }

  def dequeue = {
    inQueue.acquire
    synchronized {
      val (p, q) = queues.last
      val job = q.pop
      if (q.isEmpty) queues.remove(p)
      job
    }
  }

  def all = synchronized { queues.values.toVector.flatMap(_.iterator().asScala.toVector) }
  def clear() = synchronized { queues.clear() }
  def isEmpty = synchronized(size == 0)

}
