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

package org.openmole.core.workflow.execution.local

import org.openmole.core.event.EventDispatcher
import org.openmole.core.workflow.execution._
import org.openmole.core.workflow.job._
import org.openmole.core.workspace.{ Workspace, ConfigurationLocation }
import scala.ref.WeakReference

object LocalEnvironment {

  val DefaultNumberOfThreads = new ConfigurationLocation("LocalExecutionEnvironment", "ThreadNumber")

  Workspace += (DefaultNumberOfThreads, "1")
  var defaultNumberOfThreads = Workspace.preferenceAsInt(DefaultNumberOfThreads)

  def apply(
    nbThreads: Int = defaultNumberOfThreads,
    deinterleave: Boolean = false,
    name: Option[String] = None) = new LocalEnvironment(nbThreads, deinterleave, name)

}

class LocalEnvironment(
    val nbThreads: Int,
    val deinterleave: Boolean,
    override val name: Option[String]) extends Environment {

  @transient lazy val pool = new ExecutorPool(nbThreads, WeakReference(this))

  def nbJobInQueue = pool.waiting

  override def submit(job: Job) =
    submit(new LocalExecutionJob(this, job.moleJobs, Some(job.moleExecution)))

  def submit(moleJob: MoleJob): Unit =
    submit(new LocalExecutionJob(this, List(moleJob), None))

  private def submit(ejob: LocalExecutionJob) = {
    pool.enqueue(ejob)
    ejob.state = ExecutionState.SUBMITTED
    EventDispatcher.trigger(this, new Environment.JobSubmitted(ejob))
  }

  def submitted: Long = pool.waiting
  def running: Long = pool.running
}