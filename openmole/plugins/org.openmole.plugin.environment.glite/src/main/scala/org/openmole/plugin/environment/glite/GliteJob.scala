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

trait GliteJob extends BatchJob with BatchJobId {
  var lastShaked = System.currentTimeMillis

  override def updateState(implicit token: AccessToken) = {
    val state = super.updateState

    //if (!state.isFinal && proxyExpired < System.currentTimeMillis) throw new InternalProcessingError("Proxy for this job has expired.")

    if (state == SUBMITTED) {
      val maxNbReady = Workspace.preferenceAsInt(GliteEnvironment.JobShakingMaxReady)

      def nbReady = jobService.environment.jobRegistry.allExecutionJobs.count(_.state == READY)

      if (nbReady < maxNbReady) {

        val jobShakingAverageTime = Workspace.preferenceAsDurationInMs(GliteEnvironment.JobShakingHalfLife)
        val nbInterval = ((System.currentTimeMillis - lastShaked.toDouble) / jobShakingAverageTime)
        val probability = 1 - math.pow(0.5, nbInterval)

        lastShaked = System.currentTimeMillis

        if (Workspace.rng.nextDouble < probability) throw new ShouldBeKilledException("Killed in shaking process")
      }
    }
    state
  }
}
