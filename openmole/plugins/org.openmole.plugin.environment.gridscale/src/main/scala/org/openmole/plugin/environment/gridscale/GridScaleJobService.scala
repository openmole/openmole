/*
 * Copyright (C) 2012 reuillon
 * Copyright (C) 2014 Jonathan Passerat-Palmbach
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

import gridscale._
import org.openmole.core.workflow.execution.ExecutionState._

object GridScaleJobService {
  def translateStatus(state: JobState) =
    state match {
      case JobState.Submitted => SUBMITTED
      case JobState.Running   => RUNNING
      case JobState.Done      => DONE
      case JobState.Failed    => FAILED
    }
}

//trait GridScaleJobService extends BatchJobService {
//
//  val jobService: JS
//
//  protected def _state(j: J) = translateStatus(jobService.state(j))
//  protected def _delete(j: J) = jobService.delete(j).get
//
//  private def translateStatus(state: JobState) =
//    state match {
//      case Submitted => SUBMITTED
//      case Running   => RUNNING
//      case Done      => DONE
//      case Failed    => FAILED
//    }
//
//}
//
//trait GSJobService[JS, J] {
//  def state(js: JS, j: J): JobState
//}