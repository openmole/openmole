/*
 * Copyright (C) 2010 reuillon
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

package org.openmole.core.implementation.execution.local

import java.util.LinkedList
import java.util.concurrent.Semaphore
import org.openmole.core.implementation.execution.Environment
import org.openmole.core.implementation.job.Job
import org.openmole.core.model.execution.IExecutionJob
import org.openmole.core.model.execution.ExecutionState
import org.openmole.core.model.job.IJob
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.task.IMoleTask
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.tools.service.ThreadUtil._
import scala.collection.mutable.SynchronizedPriorityQueue
import org.openmole.core.model.execution.IEnvironment

object LocalExecutionEnvironment extends Environment {
  
  implicit def jobOrdering = new Ordering[LocalExecutionJob] {
    override def compare(left: LocalExecutionJob, right: LocalExecutionJob): Int = {
      val nbMJLeft = left.job.moleJobs.count( mj => classOf[IMoleTask].isAssignableFrom( mj.task.getClass) )
      val nbMJRight = right.job.moleJobs.count( mj => classOf[IMoleTask].isAssignableFrom( mj.task.getClass) )
          
      nbMJRight - nbMJLeft
    }
  }
  
  val DefaultNumberOfThreads = new ConfigurationLocation("LocalExecutionEnvironment", "ThreadNumber")

  Workspace += (DefaultNumberOfThreads, Integer.toString(1))
  @transient lazy val default = new LocalExecutionEnvironment(Workspace.preferenceAsInt(DefaultNumberOfThreads))
   
  override def submit(job: IJob) = default.submit(job)
  
}

class LocalExecutionEnvironment(var nbThreadVar: Int) extends Environment {
  
  import LocalExecutionEnvironment._
  
  private val jobs = new SynchronizedPriorityQueue[LocalExecutionJob]
  private val jobInQueue = new Semaphore(0)
  
  private val executers = new LinkedList[LocalExecuter]
       
  addExecuters(nbThread)
    
  private[local] def addExecuters(nbExecuters: Int) = {
    for (i <- 0 until nbExecuters) {
      val executer = new LocalExecuter(this)
      executers.synchronized {
        val thread = daemonThreadFactory.newThread(executer)
        thread.start
        executers.add(executer)
      }
    }
  }

  def nbThread: Int = nbThreadVar
  def nbJobInQueue = jobs.size
  
  def setNbThread(newNbThread: Int) = {
    synchronized {
      if (nbThread != newNbThread) {
 
        if (newNbThread > nbThread) {
          addExecuters(newNbThread - nbThread)
        } else {
          var toStop = nbThread - newNbThread
          executers.synchronized {
            val it = executers.iterator

            while (it.hasNext && toStop > 0) {
              val exe = it.next
              if (!exe.stop) {
                exe.stop = true
                it.remove
                toStop -= 1
              }
            }
          }
        }

        nbThreadVar = newNbThread
      }
    }
  }

  override def submit(job: IJob) = submit(new LocalExecutionJob(this, job, nextExecutionJobId))

  def submit(moleJob: IMoleJob): Unit = submit(new Job(moleJob.id.executionId, List(moleJob)))

  private def submit(ejob: LocalExecutionJob) = {
    EventDispatcher.trigger(this, new IEnvironment.JobSubmitted(ejob))
    ejob.state = ExecutionState.SUBMITTED
    jobs += ejob
    jobInQueue.release
  }

  private[local] def takeNextjob: LocalExecutionJob = {
    jobInQueue.acquire
    jobs.dequeue
  }
}