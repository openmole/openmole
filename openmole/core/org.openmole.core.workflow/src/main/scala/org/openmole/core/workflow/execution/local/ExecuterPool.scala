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

import org.openmole.core.tools.service.ThreadUtil

import collection.mutable
import scala.ref.WeakReference
import ThreadUtil._

class ExecuterPool(nbThreads: Int, environment: WeakReference[LocalEnvironment]) {
  private val jobs = new JobPriorityQueue

  private val executers = {
    val map = mutable.HashMap[LocalExecuter, Thread]()
    (0 until nbThreads).foreach { _ ⇒ map += createExecuter }
    map
  }

  override def finalize = executers.foreach {
    case (exe, thread) ⇒ exe.stop = true; thread.interrupt
  }

  private def createExecuter = {
    val executer = new LocalExecuter(environment)
    val thread = daemonThreadFactory.newThread(executer)
    thread.start
    (executer, thread)
  }

  private[local] def addExecuter() = synchronized { executers += createExecuter }

  private[local] def removeExecuter(ex: LocalExecuter) = synchronized { executers.remove(ex) }

  private[local] def takeNextjob: LocalExecutionJob = jobs.dequeue

  def enqueue(job: LocalExecutionJob) = jobs.enqueue(job)

  def inQueue: Int = jobs.size

}
