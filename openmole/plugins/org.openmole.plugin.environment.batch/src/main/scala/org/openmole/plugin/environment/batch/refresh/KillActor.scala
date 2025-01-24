package org.openmole.plugin.environment.batch.refresh

import org.openmole.tool.exception.Retry.retry
import org.openmole.core.workflow.execution.ExecutionState
import org.openmole.plugin.environment.batch.environment.{AccessControl, BatchEnvironment, BatchExecutionJob, BatchJobControl, JobStore}

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

object KillActor:

  def receive(msg: Kill)(implicit services: BatchEnvironment.Services, priority: AccessControl.Priority) =
    import services._
    import msg._

    BatchEnvironment.setExecutionJobSate(environment, job, ExecutionState.KILLED)

    if !JobManager.canceled(job.storedJob) && !environment.stopped
    then
      val loadedJob = JobStore.load(job.storedJob)
      JobManager.sendToMoleExecution(job.storedJob): state ⇒
        if !JobManager.jobIsFinished(state, job.storedJob) 
        then environment.submit(loadedJob)
    
    try BatchEnvironment.finishedExecutionJob(environment, job)
    finally tryKillAndClean(job, environment, batchJob)

  def tryKillAndClean(job: BatchExecutionJob, environment: BatchEnvironment, bj: Option[BatchJobControl])(using services: BatchEnvironment.Services, priority: AccessControl.Priority) =
    JobStore.clean(job.storedJob)

    def kill(bj: BatchJobControl)(using services: BatchEnvironment.Services, priority: AccessControl.Priority) = retry(services.preference(BatchEnvironment.killJobRetry))(bj.delete(priority))
    def clean(bj: BatchJobControl)(using services: BatchEnvironment.Services, priority: AccessControl.Priority) = retry(services.preference(BatchEnvironment.cleanJobRetry))(bj.clean(priority))

    try bj.foreach(kill)
    catch
      case e: Throwable ⇒ JobManager ! Error(job, environment, e, None, None)

    try bj.foreach(clean)
    catch
      case e: Throwable ⇒ JobManager ! Error(job, environment, e, None, None)



