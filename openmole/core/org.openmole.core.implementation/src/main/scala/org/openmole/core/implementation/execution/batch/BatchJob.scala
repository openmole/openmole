/*
 * Copyright (C) 2010 reuillon
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

package org.openmole.core.implementation.execution.batch

import org.openmole.commons.exception.InternalProcessingError
import org.openmole.core.implementation.internal.Activator
import org.openmole.core.model.execution.ExecutionState
import org.openmole.core.model.execution.batch.IAccessToken
import org.openmole.core.model.execution.batch.IBatchJob
import org.openmole.core.model.execution.batch.IBatchJobService
import org.openmole.core.model.execution.batch.IBatchServiceDescription
import org.openmole.core.batchservicecontrol.IUsageControl


abstract class BatchJob(val jobServiceDescription: IBatchServiceDescription) extends IBatchJob {
  
  def this(jobService: IBatchJobService[_,_]) = {
    this(jobService.description)
  }
  
  val timeStemps = new Array[Long](ExecutionState.values().length)

  var _state: ExecutionState = null
  state = ExecutionState.SUBMITED

  def state_=(state: ExecutionState): Unit = {
    setStateAndUpdateIntervals(state)
  }

  private def setStateAndUpdateIntervals(state: ExecutionState) = {
    synchronized {    
      if (this.state != state) {
        val curDate = System.currentTimeMillis
        timeStemps(state.ordinal) = curDate
        _state = state
      }
    }
  }

  override def hasBeenSubmitted: Boolean = {
    state.compareTo(ExecutionState.SUBMITED) >= 0
  }

  override def kill = {
    val token = usageControl.waitAToken
    try {
      kill(token)
    } finally {
      usageControl.releaseToken(token)
    }
  }

  override def kill(token: IAccessToken)= {
    synchronized { 
      try {
        deleteJob
      } finally {
        state = ExecutionState.KILLED
      }
    }
  }

  override def updatedState: ExecutionState = {
    val token = usageControl.waitAToken
    try {
      return getUpdatedState(token)
    } finally {
      usageControl.releaseToken(token)
    }
  }

  override def getUpdatedState(token: IAccessToken): ExecutionState = {
    synchronized {
      state = updateState
      return state
    }
  }

  override def state: ExecutionState = {
    _state
  }

  override def timeStemp(state: ExecutionState): Long = {
    timeStemps(state.ordinal())
  }

  override def lastStatusDurration: Long = {
    val currentState = state
    var previous: Long = 0
    timeStemps.slice(0, currentState.ordinal).reverse.find( _ != 0 ) match {
      case Some(stemp) => return timeStemp(currentState) - stemp
      case None => throw new InternalProcessingError("Bug should allways have submitted time stemp.")
    }

  }

  private def usageControl: IUsageControl = {
    Activator.getBatchRessourceControl().getController(jobServiceDescription).getUsageControl();
  }

  /*private IFailureControl getFailureControl() {
   return Activator.getBatchRessourceControl().getController(jobServiceDescription).getFailureControl();
   }*/

  def deleteJob
  def updateState: ExecutionState

}
