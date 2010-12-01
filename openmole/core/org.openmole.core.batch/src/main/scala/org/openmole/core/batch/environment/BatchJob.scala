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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.batch.environment

import org.openmole.commons.exception.InternalProcessingError
import org.openmole.core.model.execution.ExecutionState
import org.openmole.core.model.execution.ExecutionState._
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.control.BatchJobServiceDescription
import org.openmole.core.batch.control.BatchServiceDescription
import org.openmole.core.batch.control.BatchJobServiceControl
import org.openmole.core.batch.control.BatchJobServiceControl._


abstract class BatchJob(val jobServiceDescription: BatchJobServiceDescription) {
  
  def this(jobService: BatchJobService) = this(jobService.description)
  
  val timeStemps = new Array[Long](ExecutionState.values.length)

  var _state: ExecutionState = null
  state = SUBMITTED

  private def state_=(state: ExecutionState) = synchronized {  
    if (_state != state) {
      timeStemps(state.ordinal) = System.currentTimeMillis
      
      _state match {
        case SUBMITTED => BatchJobServiceControl.qualityControl(jobServiceDescription).decrementSubmitted
        case RUNNING => BatchJobServiceControl.qualityControl(jobServiceDescription).decrementRunning
        case _ => 
      }
      
      state match {
        case SUBMITTED => BatchJobServiceControl.qualityControl(jobServiceDescription).incrementSubmitted
        case RUNNING => BatchJobServiceControl.qualityControl(jobServiceDescription).incrementRunning
        case _ => 
      }
      
      _state = state
    }
  }

  def hasBeenSubmitted: Boolean = state.compareTo(SUBMITTED) >= 0

  def kill: Unit = withToken(jobServiceDescription,kill(_))
  
  def kill(token: AccessToken)= synchronized {
    try {
      deleteJob
    } finally {
      state = KILLED
    }
  }

  def updatedState: ExecutionState = withToken(jobServiceDescription,updatedState(_))


  def updatedState(token: AccessToken): ExecutionState = synchronized {
    state = updateState
    state
  }

  def state: ExecutionState =  _state

  def timeStemp(state: ExecutionState): Long = timeStemps(state.ordinal)
  
  def lastStateDurration: Long = {
    val currentState = state
    var previous: Long = 0
    timeStemps.view.slice(0, currentState.ordinal).reverse.find( _ != 0 ) match {
      case Some(stemp) => return timeStemp(currentState) - stemp
      case None => throw new InternalProcessingError("Bug should allways have submitted time stemp.")
    }

  }

  def deleteJob
  def updateState: ExecutionState
}
