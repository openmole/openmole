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

import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.control.BatchServiceDescription
import org.openmole.core.model.execution.ExecutionState

trait IBatchJob {
  def hasBeenSubmitted: Boolean
  //def state_= (state: ExecutionState);
  def state: ExecutionState
        
  def lastStateDurration: Long

  def timeStemp(state: ExecutionState): Long

  def jobServiceDescription: BatchServiceDescription
        
  def kill
  def kill(token: AccessToken)
	
  //void submit() throws InternalProcessingError, UserBadDataError, InterruptedException;	
  //void submit(IAccessToken token) throws InternalProcessingError, InterruptedException;
	
  def updatedState: ExecutionState
  def updatedState(token: AccessToken): ExecutionState
}
