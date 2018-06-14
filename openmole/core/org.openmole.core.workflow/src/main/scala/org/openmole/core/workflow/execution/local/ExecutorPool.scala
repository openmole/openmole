/*
 * Copyright (C) 20/11/13 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.workflow.execution.local

import java.util.UUID

import org.openmole.core.threadprovider.ThreadProvider

import collection.mutable
import scala.ref.WeakReference
import org.openmole.core.workflow.execution._
import org.openmole.core.workflow.task.MoleTask
import org.openmole.tool.collection._

object ExecutorPool {

  private def createExecutor(environment: WeakReference[LocalEnvironment], threadProvider: ThreadProvider) = {
    val executor = new LocalExecutor(environment)
    val t = threadProvider.newThread(executor, Some("executor" + UUID.randomUUID().toString))
    t.start
    (executor, t)
  }

}

class ExecutorPool(nbThreads: Int, environment: WeakReference[LocalEnvironment], threadProvider: ThreadProvider) {

  def priority(localExecutionJob: LocalExecutionJob) = localExecutionJob.moleJobs.count(mj ⇒ MoleTask.containsMoleTask(mj))

  private val jobs = PriorityQueue[LocalExecutionJob]()

  private val executors = {
    val map = mutable.HashMap[LocalExecutor, Thread]()
    (0 until nbThreads).foreach { _ ⇒ map += ExecutorPool.createExecutor(environment, threadProvider) }
    map
  }

  override def finalize = executors.foreach {
    case (exe, thread) ⇒ exe.stop = true; thread.interrupt
  }

  private[local] def addExecuter() = executors.synchronized {
    executors += ExecutorPool.createExecutor(environment, threadProvider)
  }

  private[local] def removeExecuter(ex: LocalExecutor) = executors.synchronized {
    executors.remove(ex)
  }

  private[local] def takeNextJob: LocalExecutionJob = jobs.dequeue

  private[local] def idle(localExecutor: LocalExecutor) = {
    val thread = executors.synchronized { executors.remove(localExecutor) }.get
    addExecuter()
  }

  def enqueue(job: LocalExecutionJob) = jobs.enqueue(job, priority(job))

  def waiting: Int = jobs.size

  def running: Int =
    executors.synchronized {
      executors.toList.count { case (e, t) ⇒ (t.getState == Thread.State.RUNNABLE) && !e.stop }
    }

  def stop() = {
    executors.synchronized {
      executors.foreach {
        case (exe, thread) ⇒
          exe.stop = true
          thread.stop()
      }
      executors.clear()
    }

    jobs.clear()
  }

}
