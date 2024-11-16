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

package org.openmole.core.workflow.execution

import java.util.UUID
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.tool.collection.*

import java.util.concurrent.Semaphore
import scala.collection.mutable
import scala.ref.WeakReference

object ExecutorPool:

  private def createExecutor(environment: WeakReference[LocalEnvironment], threadProvider: ThreadProvider) =
    val executor = new LocalExecutor(environment)
    val t = threadProvider.newThread(executor, Some("executor" + UUID.randomUUID().toString))
    t.start
    (executor, t)


/**
 * A pool of [[LocalExecutor]] threads
 *
 * @param nbThreads
 * @param environment
 * @param threadProvider
 */
class ExecutorPool(nbThreads: Int, environment: WeakReference[LocalEnvironment], threadProvider: ThreadProvider):
  private val jobs = new java.util.ArrayDeque[LocalExecutionJob]()
  private val semaphore = new Semaphore(0)

  private val executorMap =
    val map = mutable.HashMap[LocalExecutor, Thread]()
    (0 until nbThreads).foreach { _ => map += ExecutorPool.createExecutor(environment, threadProvider) }
    map

  def runningJobs = executorMap.keys.flatMap(_.runningJob)

  override def finalize() = executorMap.foreach {
    case (exe, thread) => exe.stop = true; thread.interrupt
  }

  private[execution] def takeNextJob: LocalExecutionJob =
    semaphore.acquire()
    jobs.synchronized(jobs.pop())

  def enqueue(job: LocalExecutionJob) =
    jobs.synchronized(jobs.add(job))
    semaphore.release()

  def waiting: Int = jobs.synchronized(jobs.size)

  def running: Int =
    executorMap.synchronized:
      executorMap.toList.count { case (e, _) => e.runningJob.isDefined }

  def stop() =
    executorMap.synchronized:
      executorMap.foreach {
        case (exe, thread) =>
          exe.stop = true
          thread.interrupt()
      }
      executorMap.clear()

    jobs.synchronized(jobs.clear())
    semaphore.drainPermits()


