/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.plugin.environment.desktop

import org.openmole.core.batch.control.JobServiceDescription
import org.openmole.core.batch.environment.BatchJob
import org.openmole.core.model.execution.ExecutionState._

class DesktopJob(jobService: DesktopJobService, jobId: String) extends BatchJob(jobService.description) {

  override def deleteJob = {
    jobService.jobSubmissionFile(jobId).delete
    jobService.timeStemps(jobId).foreach{_.delete}
  }
  
  override def updatedState: ExecutionState = {
    if(!jobService.timeStempsExists(jobId)) SUBMITTED
    else if(!jobService.resultExists(jobId)) RUNNING
    else DONE
  }
}
