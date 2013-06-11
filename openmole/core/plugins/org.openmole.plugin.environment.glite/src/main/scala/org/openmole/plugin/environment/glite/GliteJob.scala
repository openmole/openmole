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

package org.openmole.plugin.environment.glite

import org.openmole.core.batch.environment._
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.jobservice.{ BatchJob, BatchJobId }
import org.openmole.core.model.execution.ExecutionState._
import org.openmole.misc.workspace._
import org.openmole.core.batch.storage.StorageService
import org.openmole.misc.tools.service.Logger

object GliteJob extends Logger

trait GliteJob extends BatchJob with BatchJobId with StatusFiles { self ⇒
  var lastShacked = System.currentTimeMillis
  val jobService: GliteJobService

  override def updateState(implicit token: AccessToken) = {
    val state = testDone(testRunning(super.updateState))
    self.state = state

    //if (!state.isFinal && proxyExpired < System.currentTimeMillis) throw new InternalProcessingError("Proxy for this job has expired.")
    if (state == SUBMITTED) {
      val maxNbReady = Workspace.preferenceAsInt(GliteEnvironment.JobShakingMaxReady)

      def nbReady = jobService.environment.jobRegistry.allExecutionJobs.count(_.state == READY)

      if (nbReady < maxNbReady) {
        val jobShakingAverageTime = Workspace.preferenceAsDuration(GliteEnvironment.JobShakingHalfLife).toMilliSeconds
        val nbInterval = ((System.currentTimeMillis - lastShacked.toDouble) / jobShakingAverageTime)
        val probability = 1 - math.pow(0.5, nbInterval)

        lastShacked = System.currentTimeMillis

        if (Workspace.rng.nextDouble < probability) throw new ShouldBeKilledException("Killed in shaking process")
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
