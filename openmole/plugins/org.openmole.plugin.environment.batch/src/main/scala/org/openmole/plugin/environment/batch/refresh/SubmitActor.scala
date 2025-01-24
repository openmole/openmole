/*
 * Copyright (C) 2012 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

package org.openmole.plugin.environment.batch.refresh

import org.openmole.core.workflow.execution.ExecutionState._
import org.openmole.plugin.environment.batch.environment.{ AccessControl, BatchEnvironment }

object SubmitActor:

  def receive(submit: Submit)(using services: BatchEnvironment.Services, priority: AccessControl.Priority) =
    import services._

    val Submit(job, environment) = submit

    JobManager.killOr(job, Kill(job, environment, None)): () ⇒
      try
        val bj = environment.execute(job)
        BatchEnvironment.setExecutionJobSate(environment, job, SUBMITTED)
        JobManager ! Submitted(job, environment, bj)
      catch
        case e: Throwable ⇒
          JobManager ! Error(job, environment, e, None, None)
          JobManager ! Delay(Submit(job, environment), preference(BatchEnvironment.SubmitRetryInterval))

