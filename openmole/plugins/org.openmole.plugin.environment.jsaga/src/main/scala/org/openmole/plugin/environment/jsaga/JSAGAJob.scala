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

package org.openmole.plugin.environment.jsaga

import fr.in2p3.jsaga.adaptor.job.SubState
import fr.in2p3.jsaga.impl.job.service.ReconnectionException
import org.ogf.saga.job.Job
import org.openmole.core.batch.environment.BatchJob
import org.openmole.core.batch.environment.TemporaryErrorException
import org.openmole.core.model.execution.ExecutionState
import org.openmole.core.model.execution.ExecutionState.{ ExecutionState ⇒ ES }

import org.ogf.saga.monitoring.Metric
import org.ogf.saga.task.State
import org.openmole.misc.tools.service.Logger

object JSAGAJob extends Logger {

  def id(job: Job) = {
    val id = job.getAttribute(Job.JOBID)
    id.substring(id.lastIndexOf('[') + 1, id.lastIndexOf(']'))
  }

}

abstract class JSAGAJob(override val resultPath: String, jobService: JSAGAJobService) extends BatchJob(jobService) {

  //var subState: String = ""
  def job: Job = jobService.jobService.getJob(jobId)
  def jobId: String

  private def translateStatus(job: Job, state: State) = {
    //JSAGAJob.logger.fine(state.name)
    import State._

    val subState = job.getMetric(Job.JOB_STATEDETAIL).getAttribute(Metric.VALUE)

    (state match {
      case NEW ⇒ ExecutionState.SUBMITTED
      case RUNNING ⇒
        if (subState.contains(SubState.RUNNING_SUBMITTED.toString) || subState.contains(SubState.RUNNING_QUEUED.toString)) ExecutionState.SUBMITTED
        else ExecutionState.RUNNING
      case DONE ⇒ ExecutionState.DONE
      case FAILED | CANCELED | SUSPENDED | _ ⇒ ExecutionState.FAILED
    }, subState)
  }

  override def deleteJob =
    if (state == ExecutionState.SUBMITTED || state == ExecutionState.RUNNING) {
      try job.cancel
      catch {
        case e: ReconnectionException ⇒ throw new TemporaryErrorException("Service is being reconnected durring job deletion.", e)
      }
    }

  def updatedStateAndSubState = {
    val curjob = job
    try translateStatus(curjob, curjob.getState)
    catch {
      case e: ReconnectionException ⇒ throw new TemporaryErrorException("Service is being reconnected durring job satus update.", e)
    }
  }

  override def updatedState: ES = updatedStateAndSubState._1

  override def toString: String = jobId

}
