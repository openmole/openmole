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

package org.openmole.core.batch.environment

import org.openmole.misc.eventdispatcher.Event
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.core.model.execution.ExecutionState
import org.openmole.core.model.execution.ExecutionState._
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.control.ServiceDescription
import org.openmole.core.batch.control.JobServiceControl
import org.openmole.core.batch.control.JobServiceControl._
import org.openmole.core.batch.control.UsageControl._

object BatchJob {

  case class StateChanged(val newState: ExecutionState.ExecutionState, val oldState: ExecutionState.ExecutionState) extends Event[BatchJob]

}

abstract class BatchJob(val jobServiceDescription: ServiceDescription) {

  def this(jobService: JobService) = this(jobService.description)

  val timeStamps = ExecutionState.values.toList.map { v ⇒ System.currentTimeMillis }.toArray

  var _state: ExecutionState = null
  state = SUBMITTED

  private def state_=(state: ExecutionState) = synchronized {
    if (_state != state) {
      timeStamps(state.id) = System.currentTimeMillis

      _state match {
        case SUBMITTED ⇒ JobServiceControl.qualityControl(jobServiceDescription).decrementSubmitted
        case RUNNING ⇒ JobServiceControl.qualityControl(jobServiceDescription).decrementRunning
        case _ ⇒
      }

      state match {
        case SUBMITTED ⇒ JobServiceControl.qualityControl(jobServiceDescription).incrementSubmitted
        case RUNNING ⇒ JobServiceControl.qualityControl(jobServiceDescription).incrementRunning
        case DONE ⇒ JobServiceControl.qualityControl(jobServiceDescription).incrementDone
        case _ ⇒
      }

      EventDispatcher.trigger(this, new BatchJob.StateChanged(state, _state))

      _state = state
    }
  }

  def hasBeenSubmitted: Boolean = state.compareTo(SUBMITTED) >= 0

  def kill: Unit = withToken(jobServiceDescription, kill(_))

  def kill(token: AccessToken) = token.synchronized {
    synchronized {
      state = KILLED
      deleteJob
    }
  }

  def updateState: ExecutionState = withToken(jobServiceDescription, updateState(_))

  def updateState(token: AccessToken): ExecutionState = token.synchronized {
    synchronized {
      state = withFailureControl(jobServiceDescription, updatedState)
      state
    }
  }

  def state: ExecutionState = _state

  def timeStamp(state: ExecutionState): Long = timeStamps(state.id)

  def lastStateDurration: Long = {
    val currentState = state
    var previous: Long = 0
    timeStamps.view.slice(0, currentState.id).reverse.find(_ != 0) match {
      case Some(stemp) ⇒ return timeStamp(currentState) - stemp
      case None ⇒ throw new InternalProcessingError("Bug should allways have submitted time stemp.")
    }
  }

  def deleteJob
  def resultPath: String

  protected def updatedState: ExecutionState
}
