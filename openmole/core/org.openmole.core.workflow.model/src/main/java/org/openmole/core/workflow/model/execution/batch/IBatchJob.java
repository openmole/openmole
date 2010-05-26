/*
 *  Copyright (C) 2010 Romain Reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.workflow.model.execution.batch;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.workflow.model.execution.ExecutionState;

public interface IBatchJob {
	
	Long getLastStatusChangeInterval();
	Long getLastStatusChangedTime();

        boolean hasBeenSubmitted();
	void setState(ExecutionState state);
	
	ExecutionState getState();
        Long getTimeStemp(ExecutionState state);

	void kill() throws InternalProcessingError, UserBadDataError, InterruptedException;
	void kill(IAccessToken token) throws UserBadDataError, InternalProcessingError;
	
	void submit() throws InternalProcessingError, UserBadDataError, InterruptedException;	
	void submit(IAccessToken token) throws InternalProcessingError, InterruptedException;
	
	ExecutionState getUpdatedState() throws InternalProcessingError, UserBadDataError, InterruptedException;
	ExecutionState getUpdatedState(IAccessToken token) throws InternalProcessingError, UserBadDataError, InterruptedException;
	
}
