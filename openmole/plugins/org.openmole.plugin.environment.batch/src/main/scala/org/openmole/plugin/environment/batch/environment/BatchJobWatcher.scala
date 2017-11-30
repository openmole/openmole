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

package org.openmole.plugin.environment.batch.environment

import org.openmole.core.preference.Preference
import org.openmole.core.threadprovider.IUpdatableWithVariableDelay
import org.openmole.core.workflow.execution.ExecutionState._
import org.openmole.core.workflow.job.Job
import org.openmole.core.workspace.Workspace
import org.openmole.plugin.environment.batch.environment._
import org.openmole.plugin.environment.batch.refresh.{ JobManager, Kill }
import org.openmole.tool.logger.JavaLogger

import scala.concurrent.stm._
import scala.ref._

object BatchJobWatcher extends JavaLogger {

  class ExecutionJobRegistry {
    val jobs = TMap[Job, List[BatchExecutionJob]]()
    def allJobs = jobs.single.keys
    def executionJobs(job: Job): List[BatchExecutionJob] = jobs.single.getOrElse(job, List.empty)

    def update(job: Job, ejobs: List[BatchExecutionJob]) = atomic { implicit ctx ⇒
      jobs(job) = ejobs
    }
    def isEmpty: Boolean = jobs.single.isEmpty

    def register(ejob: BatchExecutionJob) = atomic { implicit ctx ⇒
      val newJobs = ejob :: jobs.getOrElse(ejob.job, List.empty)
      jobs(ejob.job) = newJobs
    }

    def removeJob(job: Job) = jobs.single -= job

    def allExecutionJobs = jobs.single.values.flatten
  }

}

class BatchJobWatcher(environment: WeakReference[BatchEnvironment], preference: Preference) extends IUpdatableWithVariableDelay { watch ⇒

  val registry = new BatchJobWatcher.ExecutionJobRegistry
  var stop = false

  import BatchJobWatcher._

  def register(job: BatchExecutionJob) = registry.register(job)

  override def update: Boolean =
    environment.get match {
      case None ⇒ false
      case Some(env) ⇒
        import env._
        Log.logger.fine("Watch jobs " + registry.allJobs.size)

        val (toKill, toSubmit) =
          atomic { implicit ctx ⇒

            val remove = registry.allJobs.filter(_.finished)
            val toKill = remove.flatMap(j ⇒ registry.executionJobs(j).filter(_.state != KILLED))
            for (j ← remove) registry.removeJob(j)

            val toSubmit =
              for (job ← registry.allJobs) yield {
                val runningJobs = registry.executionJobs(job).filter(!_.state.isFinal)
                registry(job) = runningJobs
                if (registry.executionJobs(job).isEmpty) Some(job) else None
              }
            (toKill, toSubmit.flatten)
          }

        toSubmit.foreach(env.submit)
        toKill.foreach(ej ⇒ JobManager ! Kill(ej))
        !watch.stop
    }

  def executionJobs = registry.allExecutionJobs

  def delay = preference(BatchEnvironment.CheckInterval)
}
