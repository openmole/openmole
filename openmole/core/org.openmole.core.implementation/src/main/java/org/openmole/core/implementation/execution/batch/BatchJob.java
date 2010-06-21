/*
 *  Copyright (C) 2010 reuillon
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
package org.openmole.core.implementation.execution.batch;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
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
    
    Long lastStatusChanged;
    Long lastStatusChangeInterval;
    private final IBatchServiceDescription jobServiceDescription;
    private final Map<ExecutionState, Long> timeStemps = Collections.synchronizedMap(new TreeMap<ExecutionState, Long>());

    public BatchJob(IBatchJobService<?> jobService) {
        super();
        this.jobServiceDescription = jobService.getDescription();
        setState(ExecutionState.READY);
    }

    @Override
    public void setState(ExecutionState state) {
        setStateAndUpdateIntervals(state);
    }

    private synchronized void setStateAndUpdateIntervals(ExecutionState state) {
        if (this.state != state) {
            long curDate = System.currentTimeMillis();
            timeStemps.put(state, curDate);

            if (this.state != null && !this.state.isFinal()) {
                if (lastStatusChanged != null) {
                    lastStatusChangeInterval = curDate - lastStatusChanged;
                }
            }

            lastStatusChanged = curDate;
            this.state = state;
        }
    }

    @Override
    public IBatchServiceDescription getBatchJobServiceDescription() {
        return jobServiceDescription;
    }

    @Override
    public Long getLastStatusChangedTime() {
        return lastStatusChanged;
    }

    @Override
    public Long getLastStatusChangeInterval() {
        return lastStatusChangeInterval;
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
        setState(ExecutionState.KILLED);
        deleteJob();
    }

    @Override
    public void submit() throws InternalProcessingError, InterruptedException, UserBadDataError {
        IAccessToken token = getUsageControl().waitAToken();
        try {
            submit(token);
        } finally {
            getUsageControl().releaseToken(token);
        }
    }

    @Override
    public synchronized void submit(IAccessToken token) throws InternalProcessingError, InterruptedException {
        try {
            submitJob();
            getFailureControl().success();
        } catch (InternalProcessingError e) {
            getFailureControl().failed();
            throw e;
        }
        setState(ExecutionState.SUBMITED);
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
        return timeStemps.get(state);
    }

    private IUsageControl getUsageControl() {
        return Activator.getBatchRessourceControl().getController(jobServiceDescription).getUsageControl();
    }
    
    private IFailureControl getFailureControl() {
        return Activator.getBatchRessourceControl().getController(jobServiceDescription).getFailureControl();
    }
    
    public abstract void deleteJob() throws InternalProcessingError;

    public abstract void submitJob() throws InternalProcessingError;

    public abstract ExecutionState updateState() throws InternalProcessingError;
}
