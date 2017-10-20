/*
 * Copyright (C) 2010 Romain Reuillon
 * Copyright (C) 2014 Jonathan Passerat-Palmbach
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

package org.openmole.plugin.environment.batch.jobservice

import org.openmole.core.event.{ Event, EventDispatcher }
import org.openmole.core.workflow.execution.ExecutionState

object BatchJob {
  case class StateChanged(newState: ExecutionState.ExecutionState, oldState: ExecutionState.ExecutionState) extends Event[BatchJob[_]]
}

case class BatchJob[J](id: J, resultPath: String)

//{ bj ⇒
//
//  var _state: ExecutionState = READY
//
//  protected[jobservice] def state_=(state: ExecutionState) = synchronized {
//    if (_state < state) {
//      jobService.environment.eventDispatcher.trigger(this, new BatchJob.StateChanged(state, _state))
//      _state = state
//    }
//  }
//
//  def hasBeenSubmitted: Boolean = state.compareTo(SUBMITTED) >= 0
//
//  def kill(implicit token: AccessToken) = batchJobInterface.kill()
//  def updateState(implicit token: AccessToken): ExecutionState
//
//  def kill(id: J)(implicit token: AccessToken) =
//    synchronized {
//      try if (state != KILLED) jobService.delete(id)
//      finally state = KILLED
//    }
//
//  def updateState(id: J)(implicit token: AccessToken): ExecutionState =
//    synchronized {
//      state = jobService.state(id)
//      state
//    }
//
//  def state: ExecutionState = _state
//
//}
