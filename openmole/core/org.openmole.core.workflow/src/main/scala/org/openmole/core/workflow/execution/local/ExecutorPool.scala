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
import collection.mutable
import scala.ref.WeakReference
import org.openmole.core.workflow.execution._

class ExecutorPool(nbThreads: Int, environment: WeakReference[LocalEnvironment]) {
  private val jobs = JobPriorityQueue()

  private val executors = {
    val map = mutable.HashMap[LocalExecutor, Thread]()
    (0 until nbThreads).foreach { _ ⇒ map += createExecutor }
    map
  }

  override def finalize = executors.foreach {
    case (exe, thread) ⇒ exe.stop = true; thread.interrupt
  }

  private def createExecutor = {
    val executor = new LocalExecutor(environment)
    val group = new ThreadGroup(Thread.currentThread().getThreadGroup, "executor" + UUID.randomUUID().toString)
    val t = new Thread(group, executor)
    t.setDaemon(true)
    t.start
    (executor, t)
  }

  private[local] def addExecuter() = executors.synchronized { executors += createExecutor }

  private[local] def removeExecuter(ex: LocalExecutor) = executors.synchronized { executors.remove(ex) }

  private[local] def takeNextjob: LocalExecutionJob = jobs.dequeue

  def enqueue(job: LocalExecutionJob) = jobs.enqueue(job)

  def waiting: Int = jobs.size
  def running: Int =
    executors.synchronized {
      executors.toList.count { case (e, t) ⇒ (t.getState == Thread.State.RUNNABLE) && !e.stop }
    }

}
