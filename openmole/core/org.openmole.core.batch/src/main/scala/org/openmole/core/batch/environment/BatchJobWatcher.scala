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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.batch.environment

import java.util.logging.Level
import java.util.logging.Logger
import org.openmole.core.model.execution.ExecutionState._
import org.openmole.core.model.job.IJob
import org.openmole.misc.updater.IUpdatable
import scala.collection.mutable.ListBuffer
import scala.ref.WeakReference

class BatchJobWatcher(environmentRef: WeakReference[BatchEnvironment]) extends IUpdatable {

  def this(environment: BatchEnvironment) = this(new WeakReference(environment))
  
  override def update: Boolean = {
    val environment = environmentRef.get match {
      case None => return false
      case Some(env) => env
    }
    val registry = environment.jobRegistry
    val jobGroupsToRemove = new ListBuffer[IJob]
        
    registry.synchronized  {
      for (val job <- registry.allJobs) {

        if (job.allMoleJobsFinished) {

          for (val ej <- registry.executionJobs(job)) {
            ej.kill
          }

          jobGroupsToRemove += job
        } else {

          val executionJobsToRemove = new ListBuffer[BatchExecutionJob]

          for (ej <- registry.executionJobs(job)) {
            ej.state match {
              case KILLED => executionJobsToRemove += ej
              case _ =>
            }
          }

          for (ej <- executionJobsToRemove) {
            registry.remove(ej)
          }

          if (registry.nbExecutionJobs(job) == 0) {
            try {
              environment.submit(job)
            } catch {
              case(e) => Logger.getLogger(classOf[BatchJobWatcher].getName).log(Level.SEVERE, "Submission of job failed, job isn't being executed.", e)
            }
          }
        }
      }

      for (j <- jobGroupsToRemove) {
        registry.removeJob(j)
      }
    }

    true
  }
}
