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

package org.openmole.core.batch.environment

import org.openmole.core.tools.service.Logger
import org.openmole.core.updater.IUpdatableWithVariableDelay
import org.openmole.core.workflow.execution.ExecutionState._
import org.openmole.core.workflow.job.Job
import org.openmole.core.workspace.Workspace
import collection.mutable._
import org.openmole.core.batch.refresh.{ Kill, Manage }
import scala.ref.WeakReference

object BatchJobWatcher extends Logger {
  case object Watch

  class ExecutionJobRegistry {
    val jobs = new HashMap[Job, Set[BatchExecutionJob]] with MultiMap[Job, BatchExecutionJob]

    def allJobs = jobs.keySet

    def executionJobs(job: Job) = jobs.getOrElse(job, Set.empty)

    def remove(ejob: BatchExecutionJob) = jobs.removeBinding(ejob.job, ejob)

    def isEmpty: Boolean = jobs.isEmpty

    def register(ejob: BatchExecutionJob) = jobs.addBinding(ejob.job, ejob)

    def removeJob(job: Job) = jobs -= job

    def allExecutionJobs = jobs.values.flatMap(_.toSeq)
  }

}

class BatchJobWatcher(environment: WeakReference[BatchEnvironment]) extends IUpdatableWithVariableDelay {

  val registry = new BatchJobWatcher.ExecutionJobRegistry

  import BatchJobWatcher._

  def register(job: BatchExecutionJob) = synchronized { registry.register(job) }

  override def update: Boolean = synchronized {
    val env = environment.get match {
      case None ⇒
        for (ej ← registry.allExecutionJobs) if (ej.state != KILLED) BatchEnvironment.jobManager ! Kill(ej)
        return false
      case Some(env) ⇒ env
    }

    val jobGroupsToRemove = new ListBuffer[Job]

    Log.logger.fine("Watch jobs " + registry.allJobs.size)
    for (job ← registry.allJobs) {
      if (job.finished) {
        for (ej ← registry.executionJobs(job)) if (ej.state != KILLED) BatchEnvironment.jobManager ! Kill(ej)
        jobGroupsToRemove += job
      }
      else {
        val executionJobsToRemove = new ListBuffer[BatchExecutionJob]

        for {
          ej ← registry.executionJobs(job)
          if ej.state.isFinal
        } executionJobsToRemove += ej

        for (ej ← executionJobsToRemove) registry.remove(ej)
        if (registry.executionJobs(job).isEmpty) env.submit(job)
      }
    }

    for (j ← jobGroupsToRemove) registry.removeJob(j)
    true
  }

  def executionJobs = synchronized { registry.allExecutionJobs }

  def delay = Workspace.preferenceAsDuration(BatchEnvironment.CheckInterval)
}
