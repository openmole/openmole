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

package org.openmole.plugin.environment.gridscale

import org.openmole.core.model.execution.ExecutionState._
import org.openmole.core.batch.jobservice.JobService
import org.openmole.core.batch.environment.BatchEnvironment
import fr.iscpif.gridscale.{ JobService ⇒ GSJobService, JobState, Submitted, Running, Done, Failed }

trait GridScaleJobService extends JobService {

  val jobService: GSJobService
  type J = jobService.J
  def authentication: jobService.A

  protected def _state(j: J) = translateStatus(jobService.state(j)(authentication))
  protected def _cancel(j: J) = jobService.cancel(j)(authentication)
  protected def _purge(j: J) = jobService.purge(j)(authentication)

  private def translateStatus(state: JobState) =
    state match {
      case Submitted ⇒ SUBMITTED
      case Running   ⇒ RUNNING
      case Done      ⇒ DONE
      case Failed    ⇒ FAILED
    }

}
