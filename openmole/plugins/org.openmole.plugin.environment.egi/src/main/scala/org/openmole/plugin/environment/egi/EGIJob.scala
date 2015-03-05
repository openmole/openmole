/*
 * Copyright (C) 2012 reuillon
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

package org.openmole.plugin.environment.egi

import org.openmole.core.batch.environment._
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.jobservice.{ BatchJob, BatchJobId }
import org.openmole.core.tools.service.Logger
import org.openmole.core.workflow.execution.ExecutionState._
import org.openmole.core.workspace.Workspace
import fr.iscpif.gridscale.glite.WMSJobDescription
import scala.util.{ Failure, Try }

object EGIJob extends Logger {

  import Log._

  trait EGIDebugJob extends EGIJob {
    val description: WMSJobDescription

    override def state_=(state: ExecutionState) = {
      if (_state != state) {

        try {
          state match {
            case DONE   ⇒ jobService.jobService.downloadOutputSandbox(description, id)
            case FAILED ⇒ jobService.jobService.downloadOutputSandbox(description, id)
            case _      ⇒
          }
        }
        catch {
          case e: Throwable ⇒ logger.log(WARNING, "Error retrieving output sandbox for debug job", e)
        }
      }
      super.state_=(state)
    }
  }

  def debug(gliteJob: EGIJob, _description: WMSJobDescription) =
    new EGIDebugJob {
      val storage = gliteJob.storage
      val finishedPath = gliteJob.finishedPath
      val jobService = gliteJob.jobService
      val description = _description
      val runningPath: String = gliteJob.runningPath
      val id = gliteJob.id
      val resultPath: String = gliteJob.resultPath
    }

}

trait EGIJob extends BatchJob with BatchJobId with StatusFiles { self ⇒
  var lastShacked = System.currentTimeMillis
  val jobService: EGIJobService

  override def updateState(implicit token: AccessToken) = {
    state = if (!jobService.environment.debug) testStatusFile(super.updateState) else super.updateState

    //if (!state.isFinal && proxyExpired < System.currentTimeMillis) throw new InternalProcessingError("Proxy for this job has expired.")
    if (state == SUBMITTED) {
      val maxNbReady = Workspace.preferenceAsInt(EGIEnvironment.JobShakingMaxReady)

      def nbReady = jobService.environment.executionJobs.count(_.state == READY)

      if (nbReady < maxNbReady) {
        val jobShakingAverageTime = Workspace.preferenceAsDuration(EGIEnvironment.JobShakingHalfLife)
        val nbInterval = ((System.currentTimeMillis - lastShacked.toDouble) / jobShakingAverageTime.toMillis)
        val probability = 1 - math.pow(0.5, nbInterval)

        lastShacked = System.currentTimeMillis

        if (Workspace.rng.nextDouble < probability) throw new ResubmitException("Killed in shaking process")
      }
    }
    state
  }

  override def state_=(state: ExecutionState) = synchronized {
    if (_state != state) {
      _state match {
        case SUBMITTED ⇒ jobService.decrementSubmitted
        case RUNNING   ⇒ jobService.decrementRunning
        case _         ⇒
      }

      state match {
        case SUBMITTED ⇒ jobService.incrementSubmitted
        case RUNNING   ⇒ jobService.incrementRunning
        case DONE      ⇒ jobService.incrementDone
        case _         ⇒
      }
    }
    super.state = state
  }

}
