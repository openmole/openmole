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

package org.openmole.core.workflow.execution.local

import java.util.concurrent.Semaphore
import org.openmole.core.workflow.job._
import org.openmole.core.workflow.task._
import scala.collection.immutable.TreeMap
import scala.collection.mutable.Stack

object JobPriorityQueue {
  def priority(jobs: Iterable[MoleJob]) =
    jobs.count(mj ⇒ classOf[MoleTask].isAssignableFrom(mj.task.getClass))

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
