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

package org.openmole.core.implementation.execution.local

import java.util.concurrent.Semaphore
import org.openmole.core.model.job.IJob
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.task.IMoleTask
import scala.collection.immutable.TreeMap
import scala.collection.mutable.Stack

object JobPriorityQueue {

  //  class JobQueue {
  //    val minSize = 1000
  //    val shrinkFactor = 0.5
  //    val shrinkCeil = 0.25
  //    val growthFactor = 2.
  //
  //    var jobs: Array[LocalExecutionJob] = Array.ofDim(minSize)
  //    var nextQueue = 0
  //    var nextDequeue = 0
  //    var size = 0
  //
  //    private def resize(newSize: Int) =
  //      if (newSize >= minSize) {
  //        val newJobs = Array.ofDim[LocalExecutionJob](newSize)
  //
  //        if (nextDequeue < nextQueue)
  //          jobs.slice(nextDequeue, nextQueue).zipWithIndex.foreach {
  //            case (j, i) ⇒ newJobs(i) = j
  //          }
  //        else
  //          (jobs.slice(nextDequeue, jobs.size) ++ jobs.slice(0, nextQueue)).zipWithIndex.foreach {
  //            case (j, i) ⇒ newJobs(i) = j
  //          }
  //
  //        jobs = newJobs
  //        nextDequeue = 0
  //        nextQueue = size
  //      }
  //
  //    def enqueue(job: LocalExecutionJob) = {
  //      if (size >= jobs.size) resize((jobs.size * growthFactor).toInt)
  //
  //      jobs(nextQueue) = job
  //      nextQueue = increment(nextQueue)
  //      size += 1
  //    }
  //
  //    private def increment(i: Int) = if ((i + 1) < jobs.size) i + 1 else 0
  //
  //    def dequeue = {
  //      if (isEmpty) throw new IndexOutOfBoundsException("Dequeing from an empty queue")
  //
  //      val dequeued = jobs(nextDequeue)
  //      jobs(nextDequeue) = null
  //      nextDequeue = increment(nextDequeue)
  //      size -= 1
  //
  //      if (size < (jobs.size * shrinkCeil)) resize((jobs.size * shrinkFactor).toInt)
  //      dequeued
  //    }
  //
  //    def isEmpty = size == 0
  //  }

  def priority(jobs: Iterable[IMoleJob]) =
    jobs.count(mj ⇒ classOf[IMoleTask].isAssignableFrom(mj.task.getClass))

}

import JobPriorityQueue._

class JobPriorityQueue {
  private val jobInQueue = new Semaphore(0)

  var queues = new TreeMap[Int, Stack[LocalExecutionJob]]

  def size = synchronized { queues.map { case (_, q) ⇒ q.size }.sum }

  def enqueue(job: LocalExecutionJob) = {
    synchronized {
      val p = priority(job.moleJobs)
      queues.get(p) match {
        case Some(queue) ⇒ queue.push(job)
        case None ⇒
          val q = new Stack[LocalExecutionJob]
          q.push(job)
          queues += p -> q
      }
    }
    jobInQueue.release
  }

  def dequeue = {
    jobInQueue.acquire
    synchronized {
      val (p, q) = queues.last
      val job = q.pop
      if (q.isEmpty) queues -= p
      job
    }
  }
}
