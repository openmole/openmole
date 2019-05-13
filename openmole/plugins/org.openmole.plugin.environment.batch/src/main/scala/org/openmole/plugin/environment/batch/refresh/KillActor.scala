package org.openmole.plugin.environment.batch.refresh

import org.openmole.core.tools.service.Retry.retry
import org.openmole.core.workflow.execution.ExecutionState
import org.openmole.plugin.environment.batch.environment.{ BatchEnvironment, BatchExecutionJob, BatchJobControl, JobStore }

/*
 * Copyright (C) 2019 Romain Reuillon
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

object KillActor {

  def receive(msg: Kill)(implicit services: BatchEnvironment.Services) = {
    import services._
    import msg._

    job.state = ExecutionState.KILLED

    if (!JobManager.canceled(job.storedJob) && !job.environment.stopped) {
      val loadedJob = JobStore.load(job.storedJob)
      JobManager.sendToMoleExecution(job.storedJob) { state ⇒
        if (!JobManager.jobIsFinished(state, job.storedJob)) job.environment.submit(loadedJob)
      }
    }

    try BatchEnvironment.finishedExecutionJob(job.environment, job)
    finally tryKillAndClean(job, batchJob)
  }

  def tryKillAndClean(job: BatchExecutionJob, bj: Option[BatchJobControl])(implicit services: BatchEnvironment.Services) = {
    JobStore.clean(job.storedJob)

    def kill(bj: BatchJobControl)(implicit services: BatchEnvironment.Services) = retry(services.preference(BatchEnvironment.killJobRetry))(bj.delete())
    def clean(bj: BatchJobControl)(implicit services: BatchEnvironment.Services) = retry(services.preference(BatchEnvironment.cleanJobRetry))(bj.clean())

    try bj.foreach(kill) catch {
      case e: Throwable ⇒ JobManager ! Error(job, e, None)
    }

    try bj.foreach(clean) catch {
      case e: Throwable ⇒ JobManager ! Error(job, e, None)
    }
  }
}
