/*
 * Copyright (C) 2010 reuillon
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

package org.openmole.plugin.environment.jsaga

import fr.in2p3.jsaga.adaptor.job.SubState
import org.ogf.saga.error.TimeoutException
import org.ogf.saga.job.Job
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.core.implementation.execution.batch.BatchJob
import org.openmole.core.model.execution.ExecutionState

import org.ogf.saga.monitoring.Metric
import org.ogf.saga.task.State

class JSAGAJob(jobId: String, jobService: JSAGAJobService[_,_]) extends BatchJob(jobService) {
   
  def job: Job = {
    synchronized {
      try {
        return jobService.getJobServiceCache.getJob(jobId)
      } catch {
        case (e: Exception) => throw new InternalProcessingError(e)
      } 
    }
  }

  private def translateStatus(job: Job, state: State): ExecutionState = {
    import State._
    
    state match {
      case NEW => ExecutionState.SUBMITED
      case RUNNING =>
        val subState = try {
          job.getMetric(fr.in2p3.jsaga.impl.job.instance.AbstractSyncJobImpl.JOB_SUBSTATE).getAttribute(Metric.VALUE);
        } catch {
          case (e: Exception) => throw new InternalProcessingError(e)
        }

        if (!subState.equals(SubState.RUNNING_ACTIVE.toString)) {
          ExecutionState.SUBMITED
        } else {
          ExecutionState.RUNNING
        }
      case DONE => ExecutionState.DONE
      case FAILED | CANCELED | SUSPENDED | _ => ExecutionState.FAILED
    }
  }

  override def deleteJob =  {
    try {
      if (state == ExecutionState.SUBMITED || state == ExecutionState.RUNNING) {
        job.cancel
      }
    } catch {
      case (e: Exception) => throw new InternalProcessingError(e)
    }
  }

  
  override def updateState: ExecutionState = {
    try {
      val curjob = job
      return translateStatus(curjob, curjob.getState)
    } catch {
      case (e: TimeoutException) => //setState(ExecutionState.FAILED);
        throw new InternalProcessingError(e);
      case (e: Exception) => state = ExecutionState.FAILED
        throw new InternalProcessingError(e);
    }
  }

  override def toString: String = {
    jobId
  }
}
