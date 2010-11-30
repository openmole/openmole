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

package org.openmole.plugin.environment.jsaga

import fr.in2p3.jsaga.adaptor.job.SubState
import fr.in2p3.jsaga.impl.job.service.ReconnectionException
import java.util.logging.Logger
import org.ogf.saga.error.TimeoutException
import org.ogf.saga.job.Job
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.core.batch.environment.BatchJob
import org.openmole.core.model.execution.ExecutionState

import org.ogf.saga.monitoring.Metric
import org.ogf.saga.task.State

class JSAGAJob(jobId: String, jobService: JSAGAJobService[_,_]) extends BatchJob(jobService) {
   
  def job: Job = jobService.jobServiceCache.getJob(jobId)

  private def translateStatus(job: Job, state: State): ExecutionState = {
    import State._
    
    state match {
      case NEW => ExecutionState.SUBMITED
      case RUNNING =>
        val subState = job.getMetric(fr.in2p3.jsaga.impl.job.instance.AbstractSyncJobImpl.JOB_SUBSTATE).getAttribute(Metric.VALUE);
       
        if (!subState.equals(SubState.RUNNING_ACTIVE.toString)) ExecutionState.SUBMITED
        else ExecutionState.RUNNING
        
      case DONE => ExecutionState.DONE
      case FAILED | CANCELED | SUSPENDED | _ => ExecutionState.FAILED
    }
  }

  override def deleteJob =  {
    if (state == ExecutionState.SUBMITED || state == ExecutionState.RUNNING) {
      var deleted = false
      do {
        try {
          job.cancel
          deleted = true
        } catch {
          case e: ReconnectionException => 
            Logger.getLogger(classOf[JSAGAJob].getName).info("Job service reconnecting, retrying in 10s.")
            Thread.sleep(1000)
        }
      } while(!deleted)
    }
  }

  override def updateState: ExecutionState = {
    val curjob = job
    return translateStatus(curjob, curjob.getState)
  }

  override def toString: String = jobId
  
}
