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

package org.openmole.core.implementation.execution.local

import java.util.concurrent.Semaphore
import org.openmole.core.model.job.IJob
import org.openmole.core.model.task.IMoleTask
import scala.collection.immutable.TreeMap

object JobPriorityQueue {
  
  class JobQueue {
    val minSize = 1000
    val shrinkFactor = 0.5
    val growthFactor = 2.
    
    var jobs: Array[LocalExecutionJob] = Array.ofDim(minSize)
    var nextSlot = 0
     
    private def resize(newSize: Int) = 
      if(newSize >= minSize) {
        val newJobs = Array.ofDim[LocalExecutionJob](newSize)
        for(i <- 0 until nextSlot) newJobs(i) = jobs(i)
        jobs = newJobs
      }
    
    def size = nextSlot
    
    def enqueue(job: LocalExecutionJob) = {
      if(nextSlot > jobs.size - 1) resize((jobs.size * growthFactor).toInt)
      jobs(nextSlot) = job
      nextSlot += 1
    }
    
    def dequeue = {
      nextSlot -= 1
      val dequeued = jobs(nextSlot)
      jobs(nextSlot) = null
      if(nextSlot < (jobs.size * shrinkFactor)) resize(nextSlot)
      dequeued
    }
    
    def isEmpty = nextSlot == 0
  }
  
  def priority(job: IJob) = job.moleJobs.count( mj => classOf[IMoleTask].isAssignableFrom( mj.task.getClass) )
  
}

import JobPriorityQueue._

class JobPriorityQueue {
  private val jobInQueue = new Semaphore(0)
    
  var queues = new TreeMap[Int, JobQueue]
   
  def size = synchronized { queues.map{case(_, q) => q.size}.sum }
  
  def enqueue(job: LocalExecutionJob) = {
    synchronized {
      val p = priority(job.job)
      queues.get(p) match {
        case Some(queue) => queue.enqueue(job)
        case None =>
          val q = new JobQueue
          q.enqueue(job)
          queues += p -> q
      }
    }
    jobInQueue.release
  }
    
  def dequeue = {
    jobInQueue.acquire
    synchronized {
      val (p, q) = queues.last
      val job = q.dequeue
      if(q.isEmpty) queues -= p
      job
    }
  }
}
