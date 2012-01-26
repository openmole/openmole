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

class LocalExecutionEnvironment(val nbThreads: Int) extends Environment {
  
  import LocalExecutionEnvironment._
  
  private val jobs = new SynchronizedPriorityQueue[LocalExecutionJob]
  private val jobInQueue = new Semaphore(0)
  
  private var executers = List.empty[(LocalExecuter, Thread)]
       
  addExecuters(nbThreads)
  
  override def finalize = executers.foreach{
    case(exe, thread) => exe.stop = true; thread.interrupt
  }
    
  private[local] def addExecuters(nbExecuters: Int) = synchronized {
    for (i <- 0 until nbExecuters) {
      val executer = new LocalExecuter(this)
      val thread = daemonThreadFactory.newThread(executer)
      thread.start
      executers ::= executer -> thread
    }
    executers = executers.filterNot(_._1.stop)
  }

  def nbJobInQueue = jobs.size

  override def submit(job: IJob) = submit(new LocalExecutionJob(this, job))

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