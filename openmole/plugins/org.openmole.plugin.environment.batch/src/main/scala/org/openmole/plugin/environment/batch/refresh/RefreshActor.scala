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

import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.workflow.execution.ExecutionState._
import org.openmole.plugin.environment.batch.environment.*
import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

object RefreshActor:

  def receive(refresh: Refresh)(using services: BatchEnvironment.Services, priority: AccessControl.Priority) =
    import services._

    val Refresh(job, environment, bj, delay, updateErrorsInARow) = refresh

    JobManager.killOr(environment, job, Kill(job, environment, Some(bj))): () =>
      try
        val oldState = BatchEnvironment.executionSate(environment, job)
        BatchEnvironment.setExecutionSate(environment, job, BatchJobControl.updateState(bj))
        val newState = BatchEnvironment.executionSate(environment, job)
        newState match
          case DONE => JobManager ! GetResult(job, environment, bj.resultPath, bj)
          case FAILED =>
            val exception = new InternalProcessingError(s"""Job status is FAILED""".stripMargin)
            val stdOutErr = BatchJobControl.tryStdOutErr(bj).toOption
            JobManager ! Error(job, environment, exception, stdOutErr, None)
            JobManager ! Kill(job, environment, Some(bj))
          case SUBMITTED | RUNNING =>
            val updateInterval = bj.updateInterval
            val newDelay =
              if oldState == newState
              then (delay + updateInterval.incrementUpdateInterval) min updateInterval.maxUpdateInterval
              else updateInterval.minUpdateInterval
            JobManager ! Delay(Refresh(job, environment, bj, newDelay, 0), newDelay)
          case KILLED =>
          case _      => throw new InternalProcessingError(s"Job ${job} is in state ${newState} while being refreshed")

      catch
        case e: Throwable =>
          if updateErrorsInARow >= preference(BatchEnvironment.MaxUpdateErrorsInARow)
          then
            JobManager ! Error(job, environment, e, BatchJobControl.tryStdOutErr(bj).toOption, None)
            JobManager ! Kill(job, environment, Some(bj))
          else
            Logger.fine(s"${updateErrorsInARow + 1} errors in a row during job refresh", e)
            JobManager ! Delay(Refresh(job, environment, bj, delay, updateErrorsInARow + 1), delay)


