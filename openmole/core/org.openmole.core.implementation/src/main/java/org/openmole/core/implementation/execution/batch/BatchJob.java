/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Affero GNU General Public License as published by
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
package org.openmole.core.implementation.execution.batch;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.implementation.internal.Activator;
import org.openmole.core.model.execution.ExecutionState;
import org.openmole.core.model.execution.batch.IAccessToken;
import org.openmole.core.model.execution.batch.IBatchJob;
import org.openmole.core.model.execution.batch.IBatchJobService;
import org.openmole.core.model.execution.batch.IBatchServiceDescription;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.batchservicecontrol.IFailureControl;
import org.openmole.core.batchservicecontrol.IUsageControl;

public abstract class BatchJob implements IBatchJob {

    ExecutionState state;
    private final IBatchServiceDescription jobServiceDescription;
    private final long timeStemps[] = new long[ExecutionState.values().length];
    //private final Map<ExecutionState, Long> timeStemps = Collections.synchronizedMap(new EnumMap<ExecutionState, Long>(ExecutionState.class));

    public BatchJob(IBatchJobService jobService) {
        super();
        this.jobServiceDescription = jobService.getDescription();
        setState(ExecutionState.SUBMITED);
    }
    
    @Override
    public void setState(ExecutionState state) {
        setStateAndUpdateIntervals(state);
    }

    private synchronized void setStateAndUpdateIntervals(ExecutionState state) {
        if (this.state != state) {
            long curDate = System.currentTimeMillis();
            timeStemps[state.ordinal()] = curDate;
            this.state = state;
        }
    }

    @Override
    public IBatchServiceDescription getBatchJobServiceDescription() {
        return jobServiceDescription;
    }

    @Override
    public boolean hasBeenSubmitted() {
        return getState().compareTo(ExecutionState.SUBMITED) >= 0;
    }

    @Override
    public void kill() throws InternalProcessingError, InterruptedException, UserBadDataError {
        IAccessToken token = getUsageControl().waitAToken();
        try {
            kill(token);
        } finally {
            getUsageControl().releaseToken(token);
        }
    }

    @Override
    public synchronized void kill(IAccessToken token) throws InternalProcessingError {
        try {
            deleteJob();
        } finally {
            setState(ExecutionState.KILLED);
        }
    }

    @Override
    public ExecutionState getUpdatedState() throws InternalProcessingError, InterruptedException, UserBadDataError {
        IAccessToken token = getUsageControl().waitAToken();
        try {
            return getUpdatedState(token);
        } finally {
            getUsageControl().releaseToken(token);
        }
    }

    @Override
    public synchronized ExecutionState getUpdatedState(IAccessToken token) throws InternalProcessingError, InterruptedException {
        setState(updateState());
        return state;
    }

    @Override
    public ExecutionState getState() {
        return state;
    }

    @Override
    public Long getTimeStemp(ExecutionState state) {
        return timeStemps[state.ordinal()];
    }

    @Override
    public long getLastStatusDurration() {
        ExecutionState currentState = state;
        int ordinal = currentState.ordinal();
        Long previous = null;

        while (--ordinal >= 0) {
            ExecutionState previousState = ExecutionState.values()[ordinal];
            previous = getTimeStemp(previousState);
            if (previous != null) {
                break;
            }
        }

        return getTimeStemp(currentState) - previous;
    }

    private IUsageControl getUsageControl() {
        return Activator.getBatchRessourceControl().getController(jobServiceDescription).getUsageControl();
    }

    /*private IFailureControl getFailureControl() {
        return Activator.getBatchRessourceControl().getController(jobServiceDescription).getFailureControl();
    }*/

    public abstract void deleteJob() throws InternalProcessingError;

    public abstract ExecutionState updateState() throws InternalProcessingError;
}
