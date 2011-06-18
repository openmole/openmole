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

import org.openmole.misc.exception.InternalProcessingError
import org.openmole.core.model.execution.ExecutionState
import org.openmole.core.model.execution.ExecutionState._
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.control.JobServiceDescription
import org.openmole.core.batch.control.JobServiceControl
import org.openmole.core.batch.control.JobServiceControl._


abstract class BatchJob(val jobServiceDescription: JobServiceDescription) {
  
  def this(jobService: JobService) = this(jobService.description)
  
  val timeStemps = new Array[Long](ExecutionState.values.size)

  var _state: ExecutionState = null
  state = SUBMITTED

  private def state_=(state: ExecutionState) = synchronized {  
    if (_state != state) {
      timeStemps(state.id) = System.currentTimeMillis
      
      _state match {
        case SUBMITTED => JobServiceControl.qualityControl(jobServiceDescription).decrementSubmitted
        case RUNNING => JobServiceControl.qualityControl(jobServiceDescription).decrementRunning
        case _ => 
      }
      
      state match {
        case SUBMITTED => JobServiceControl.qualityControl(jobServiceDescription).incrementSubmitted
        case RUNNING => JobServiceControl.qualityControl(jobServiceDescription).incrementRunning
        case _ => 
      }
      
      _state = state
    }
  }

  def hasBeenSubmitted: Boolean = state.compareTo(SUBMITTED) >= 0

  def kill: Unit = withToken(jobServiceDescription,kill(_))
  
  def kill(token: AccessToken) = synchronized {
    state = KILLED
    deleteJob
  }

  def updateState: ExecutionState = withToken(jobServiceDescription,updateState(_))

  def updateState(token: AccessToken): ExecutionState = synchronized {
    state = withFailureControl(jobServiceDescription, updatedState)
    state
  }

  def state: ExecutionState = _state

  def timeStemp(state: ExecutionState): Long = timeStemps(state.id)
  
  def lastStateDurration: Long = {
    val currentState = state
    var previous: Long = 0
    timeStemps.view.slice(0, currentState.id).reverse.find( _ != 0 ) match {
      case Some(stemp) => return timeStemp(currentState) - stemp
      case None => throw new InternalProcessingError("Bug should allways have submitted time stemp.")
    }
  }

  def deleteJob
  def resultPath: String
  
  protected def updatedState: ExecutionState
}
