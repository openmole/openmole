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

import java.util
import java.util.concurrent.Semaphore
import scala.jdk.CollectionConverters._

object PriorityQueue:
  def apply[T](fifo: Boolean = false) = new PriorityQueue[T](fifo)

  sealed trait InnerQueue[T]

  case class FIFO[T](linkedList: util.LinkedList[T] = new util.LinkedList[T]) extends InnerQueue[T]

  case class FILO[T](stack: util.Stack[T] = new util.Stack[T]) extends InnerQueue[T]

  def add[T](innerQueue: InnerQueue[T], t: T) =
    innerQueue match
      case FILO(s) ⇒ s.push(t)
      case FIFO(l) ⇒ l.add(t)

  def pool[T](innerQueue: InnerQueue[T]) =
    innerQueue match
      case FILO(s) ⇒ s.pop()
      case FIFO(l) ⇒ l.poll()

  def clear[T](innerQueue: InnerQueue[T]) =
    innerQueue match
      case FILO(s) ⇒ s.clear()
      case FIFO(l) ⇒ l.clear()

  def size[T](innerQueue: InnerQueue[T]) =
    innerQueue match
      case FILO(s) ⇒ s.size()
      case FIFO(l) ⇒ l.size()

  def isEmpty[T](innerQueue: InnerQueue[T]) =
    innerQueue match
      case FILO(s) ⇒ s.isEmpty()
      case FIFO(l) ⇒ l.isEmpty()

  def toVector[T](innerQueue: InnerQueue[T]) =
    innerQueue match
      case FILO(s) ⇒ s.iterator().asScala.toVector
      case FIFO(l) ⇒ l.iterator().asScala.toVector

class PriorityQueue[T](fifo: Boolean):

  private var inQueue = 0
  val queues = (new java.util.TreeMap[Int, PriorityQueue.InnerQueue[T]]).asScala

  def size: Int = synchronized(inQueue)

  def enqueue(e: T, priority: Int) =
    synchronized:
      queues.get(priority) match
        case Some(queue) ⇒ PriorityQueue.add(queue, e)
        case None ⇒
          val q: PriorityQueue.InnerQueue[T] = if !fifo then PriorityQueue.FILO[T]() else PriorityQueue.FIFO[T]()
          PriorityQueue.add(q, e)
          queues.put(priority, q)
      inQueue += 1

  def dequeue(): Option[T] =
    synchronized:
      var res: Option[T] = None

      queues.lastOption.foreach: (p, q) =>
        val job = PriorityQueue.pool(q)
        res = Some(job)
        inQueue -= 1
        if PriorityQueue.isEmpty(q) then queues.remove(p)
        job
      
      res

  def all =
    synchronized:
      queues.values.toVector.flatMap(PriorityQueue.toVector)

  def clear() =
    synchronized:
      queues.clear()

  def isEmpty = synchronized(size == 0)


object BlockingPriorityQueue:
  def apply[T](fifo: Boolean = false) = new BlockingPriorityQueue[T](fifo)

class BlockingPriorityQueue[T](fifo: Boolean):

  private val inQueue = new Semaphore(0)

  val queues = (new java.util.TreeMap[Int, PriorityQueue.InnerQueue[T]]).asScala

  def size: Int = inQueue.availablePermits

  def enqueue(e: T, priority: Int) =
    synchronized:
      queues.get(priority) match
        case Some(queue) ⇒ PriorityQueue.add(queue, e)
        case None ⇒
          val q: PriorityQueue.InnerQueue[T] = if !fifo then PriorityQueue.FILO[T]() else PriorityQueue.FIFO[T]()
          PriorityQueue.add(q, e)
          queues.put(priority, q)
    inQueue.release()

  def dequeue() =
    inQueue.acquire()
    synchronized:
      val (p, q) = queues.last
      val job = PriorityQueue.pool(q)
      if PriorityQueue.isEmpty(q) then queues.remove(p)
      job

  def all =
    synchronized:
      queues.values.toVector.flatMap(PriorityQueue.toVector)

  def clear() =
    synchronized:
      queues.clear()
    inQueue.drainPermits()

  def isEmpty = synchronized(size == 0)

