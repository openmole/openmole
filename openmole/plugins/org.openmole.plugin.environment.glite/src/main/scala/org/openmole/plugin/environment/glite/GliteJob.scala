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

package org.openmole.plugin.environment.glite

import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.workspace.Workspace
import org.openmole.plugin.environment.jsaga.JSAGAJob
import java.io.File
import org.openmole.core.batch.environment.ShouldBeKilledException
import org.openmole.core.model.execution.ExecutionState._
import fr.in2p3.jsaga.adaptor.job.SubState
import org.openmole.core.model.execution.ExecutionState
import scala.io.Source

class GliteJob(
    val jobId: String,
    resultPath: String,
    jobService: GliteJobService,
    proxyExpired: Long) extends JSAGAJob(resultPath, jobService) {

  var lastUpdate = System.currentTimeMillis

  override def updatedState: ExecutionState = {

    val (state, subState) = super.updatedStateAndSubState

    if (!state.isFinal && proxyExpired < System.currentTimeMillis) throw new InternalProcessingError("Proxy for this job has expired.")

    if (state == SUBMITTED) {
      val jobShakingAverageTime = Workspace.preferenceAsDurationInMs(GliteEnvironment.JobShakingAverageTime)
      val maxNbReady = Workspace.preferenceAsInt(GliteEnvironment.JobShakingAverageMaxReady)

      val nbInterval = ((System.currentTimeMillis - lastUpdate.toDouble) / jobShakingAverageTime)

      val probability = 1 - math.pow(0.5, nbInterval)

      def nbReady = jobService.environment.jobRegistry.allExecutionJobs.count(_.state == READY)

      if (Workspace.rng.nextDouble < probability && nbReady < maxNbReady)
        throw new ShouldBeKilledException("Killed in shaking process")
    }

    lastUpdate = System.currentTimeMillis
    state
  }

}
