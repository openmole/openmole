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

package org.openmole.core.implementation.execution.local

import org.openmole.core.implementation.execution.ExecutionJob
import org.openmole.core.implementation.tools.TimeStamp
import org.openmole.core.model.execution.ExecutionState
import org.openmole.core.model.execution.ExecutionState._
import org.openmole.core.model.execution.IExecutionJobId
import org.openmole.core.model.job.IJob
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.core.model.execution.IExecutionJob

class LocalExecutionJob(environment: LocalExecutionEnvironment, job: IJob, id: IExecutionJobId) extends ExecutionJob(environment, job, id) {
  private var _state: ExecutionState = READY
    
  override def state = _state
  
  def state_=(state: ExecutionState) {
    timeStamps += (new TimeStamp(state))
    EventDispatcher.trigger(this, new IExecutionJob.StateChanged(state, this.state))
    _state = state
  }
}
