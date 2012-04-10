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

package org.openmole.core.batch.environment

import akka.actor.Actor
import org.openmole.core.batch.refresh.Kill
import org.openmole.core.model.execution.ExecutionState._
import org.openmole.core.model.job.IJob
import org.openmole.misc.tools.service.Logger
import scala.collection.mutable.ListBuffer

object BatchJobWatcher extends Logger {
  case class Watch
}

class BatchJobWatcher(environment: BatchEnvironment) extends Actor {

  import BatchJobWatcher._
  
  def receive = {
    case Watch =>
      val registry = environment.jobRegistry
      val jobGroupsToRemove = new ListBuffer[IJob]
    
      registry.synchronized  {
        for (val job <- registry.allJobs) {

          if (job.allMoleJobsFinished) {
            for (ej <- registry.executionJobs(job)) environment.jobManager ! Kill(ej)
            jobGroupsToRemove += job
          } else {
            val executionJobsToRemove = new ListBuffer[BatchExecutionJob]

            for (ej <- registry.executionJobs(job) if (ej.state.isFinal)) executionJobsToRemove += ej
            for (ej <- executionJobsToRemove) registry.remove(ej)

            if (registry.executionJobs(job).isEmpty)  environment.submit(job)
          }
        }

        for (j <- jobGroupsToRemove) registry.removeJob(j)
      }
  }
}