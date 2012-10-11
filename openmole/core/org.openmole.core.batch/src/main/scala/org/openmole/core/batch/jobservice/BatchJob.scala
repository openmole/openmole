/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.batch.jobservice

import org.openmole.misc.eventdispatcher.Event
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.core.model.execution.ExecutionState
import org.openmole.core.model.execution.ExecutionState._
import org.openmole.core.batch.control._
import org.openmole.core.batch.environment._

object BatchJob {
  case class StateChanged(val newState: ExecutionState.ExecutionState, val oldState: ExecutionState.ExecutionState) extends Event[BatchJob]
}

trait BatchJob { bj ⇒

  val jobService: JobService
  def resultPath: String

  val timeStamps = ExecutionState.values.toList.map { v ⇒ System.currentTimeMillis }.toArray

  var _state: ExecutionState = null

  protected[jobservice] def state_=(state: ExecutionState) = synchronized {
    if (_state != state) {
      timeStamps(state.id) = System.currentTimeMillis

      _state match {
        case SUBMITTED ⇒ JobServiceControl.qualityControl(jobService.id).map(_.decrementSubmitted)
        case RUNNING ⇒ JobServiceControl.qualityControl(jobService.id).map(_.decrementRunning)
        case _ ⇒
      }

      state match {
        case SUBMITTED ⇒ JobServiceControl.qualityControl(jobService.id).map(_.incrementSubmitted)
        case RUNNING ⇒ JobServiceControl.qualityControl(jobService.id).map(_.incrementRunning)
        case DONE ⇒ JobServiceControl.qualityControl(jobService.id).map(_.incrementDone)
        case _ ⇒
      }

      EventDispatcher.trigger(this, new BatchJob.StateChanged(state, _state))

      _state = state
    }
  }

  def hasBeenSubmitted: Boolean = state.compareTo(SUBMITTED) >= 0

  def kill(implicit token: AccessToken)
  def updateState(implicit token: AccessToken): ExecutionState
  def purge(implicit token: AccessToken)

  def kill(id: jobService.J)(implicit token: AccessToken) = token.synchronized {
    synchronized {
      try if (state == SUBMITTED || state == RUNNING) jobService.cancel(id)
      finally state = KILLED
    }
  }

  def updateState(id: jobService.J)(implicit token: AccessToken): ExecutionState = token.synchronized {
    synchronized {
      state = jobService.state(id)
      state
    }
  }

  def state: ExecutionState = _state

  def timeStamp(state: ExecutionState): Long = timeStamps(state.id)

  def purge(id: jobService.J)(implicit token: AccessToken) = token.synchronized { jobService.purge(id) }

  def withToken[T](f: AccessToken ⇒ T) = UsageControl.withToken(jobService.id)(f)
}
