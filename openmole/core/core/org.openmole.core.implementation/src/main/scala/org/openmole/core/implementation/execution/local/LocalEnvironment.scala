/*
 * Copyright (C) 2010 Romain Reuillon
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
import org.openmole.core.implementation.job.Job
import org.openmole.core.model.execution._
import org.openmole.core.model.job._
import org.openmole.core.model.task._
import org.openmole.misc.workspace._
import org.openmole.misc.eventdispatcher._
import org.openmole.misc.tools.service.ThreadUtil._
import scala.collection.immutable.TreeMap
import ref.WeakReference

object LocalEnvironment extends Environment {

  val DefaultNumberOfThreads = new ConfigurationLocation("LocalExecutionEnvironment", "ThreadNumber")

  Workspace += (DefaultNumberOfThreads, Integer.toString(1))

  var initializationNumberOfThread: Option[Int] = None
  def numberOfThread = initializationNumberOfThread.getOrElse(Workspace.preferenceAsInt(DefaultNumberOfThreads))

  @transient lazy val default = new LocalEnvironment(numberOfThread)

  override def submit(job: IJob) = default.submit(job)

  def apply(nbThreads: Int) = new LocalEnvironment(nbThreads)

}

class LocalEnvironment(val nbThreads: Int) extends Environment {

  import LocalEnvironment._

  private val jobs = new JobPriorityQueue

  private var executers = List.empty[(LocalExecuter, Thread)]

  addExecuters(nbThreads)

  override def finalize = executers.foreach {
    case (exe, thread) ⇒ exe.stop = true; thread.interrupt
  }

  private[local] def addExecuters(nbExecuters: Int) = synchronized {
    for (i ← 0 until nbExecuters) {
      val executer = new LocalExecuter(WeakReference(this))
      val thread = daemonThreadFactory.newThread(executer)
      thread.start
      executers ::= executer -> thread
    }
    executers = executers.filterNot(_._1.stop)
  }

  def nbJobInQueue = jobs.size

  override def submit(job: IJob) = submit(new LocalExecutionJob(this, job.moleJobs))

  def submit(moleJob: IMoleJob): Unit = submit(new LocalExecutionJob(this, List(moleJob)))

  private def submit(ejob: LocalExecutionJob) = {
    EventDispatcher.trigger(this, new Environment.JobSubmitted(ejob))
    ejob.state = ExecutionState.SUBMITTED
    jobs.enqueue(ejob)
  }

  private[local] def takeNextjob: LocalExecutionJob = jobs.dequeue

}