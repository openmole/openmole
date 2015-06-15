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

import scala.collection.immutable.TreeMap
import scala.collection.mutable.Stack

object PriorityQueue {

  def apply[T](p: T ⇒ Int) =
    new PriorityQueue[T] {
      def priority = p
    }

}

trait PriorityQueue[T] {
  private val inQueue = new Semaphore(0)

  def priority: T ⇒ Int

  var queues = new TreeMap[Int, Stack[T]]

  def size: Int = synchronized { queues.map { case (_, q) ⇒ q.size }.sum }

  def enqueue(e: T) = {
    synchronized {
      val p = priority(e)
      queues.get(p) match {
        case Some(queue) ⇒ queue.push(e)
        case None ⇒
          val q = new Stack[T]
          q.push(e)
          queues += p -> q
      }
    }
    inQueue.release
  }

  def dequeue = {
    inQueue.acquire
    synchronized {
      val (p, q) = queues.last
      val job = q.pop
      if (q.isEmpty) queues -= p
      job
    }
  }
}
