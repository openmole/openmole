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

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.misc.executorservice.ExecutorType;
import org.openmole.core.implementation.internal.Activator;
import org.openmole.core.model.execution.ExecutionState;
import org.openmole.core.model.execution.batch.IAccessToken;
import org.openmole.core.model.execution.batch.IBatchExecutionJob;
import org.openmole.core.model.execution.batch.IBatchJob;
import org.openmole.core.model.execution.batch.IBatchJobService;
import org.openmole.core.model.execution.batch.SampleType;
import org.openmole.commons.tools.structure.Duo;
import org.openmole.misc.updater.IUpdatable;
import org.openmole.misc.updater.IUpdatableFuture;
import org.openmole.misc.workspace.ConfigurationLocation;
import org.openmole.misc.backgroundexecutor.IBackgroundExecution;
import org.openmole.core.implementation.execution.ExecutionJob;
import org.openmole.core.model.execution.IJobStatisticCategory;
import org.openmole.core.model.file.IURIFile;
import org.openmole.core.model.job.IJob;
import org.openmole.core.model.mole.IExecutionContext;

public class BatchExecutionJob<JS extends IBatchJobService> extends ExecutionJob<BatchEnvironment<JS>> implements IBatchExecutionJob<BatchEnvironment<JS>>, IUpdatable {

    final static String configurationGroup = BatchExecutionJob.class.getSimpleName();
    final static ConfigurationLocation UpdateInterval = new ConfigurationLocation(configurationGroup, "UpdateInterval");

    static {
        Activator.getWorkspace().addToConfigurations(UpdateInterval, "PT2M");
    }

    long updateInterval;
    IUpdatableFuture future;
    IBatchJob batchJob;
    Boolean killed = false;
    CopyToEnvironment initStorage;
    GetResultFromEnvironment getResult;

    transient IBackgroundExecution initStorageExec;
    transient IBackgroundExecution finalizeExecution;
    transient IURIFile inputFile;
    transient IURIFile outputFile;
    transient Lock killLock;

    public BatchExecutionJob(BatchEnvironment<JS> executionEnvironment, IJob job, IExecutionContext executionContext) throws InternalProcessingError {
        super(executionEnvironment, job);
        this.updateInterval = Activator.getWorkspace().getPreferenceAsDurationInMs(UpdateInterval);
        this.initStorage = new CopyToEnvironment(executionEnvironment, job, executionContext);
    }

    public void setFuture(IUpdatableFuture future) {
        this.future = future;
    }

    @Override
    public IBatchJob getBatchJob() {
        return batchJob;
    }

    private ExecutionState updateAndGetState() throws InternalProcessingError, UserBadDataError, InterruptedException, TimeoutException {
        if (getBatchJob() == null) {
            return ExecutionState.READY;
        }

        ExecutionState oldState = getBatchJob().getState();

        if (!oldState.isFinal()) {
            ExecutionState newState = getBatchJob().getUpdatedState();

            if (oldState == ExecutionState.SUBMITED && newState == ExecutionState.RUNNING) {
                IJobStatisticCategory capsule = getEnvironment().getJobRegistries().getJobStatisticCategory(this);
                IExecutionContext executionContext = getEnvironment().getJobRegistries().getExecutionContext(this);
                getEnvironment().sample(SampleType.WAITING, getBatchJob().getLastStatusChangeInterval(), executionContext, capsule);
            }
        }

        return getState();
    }

    @Override
    public ExecutionState getState() {
        if (killed) {
            return ExecutionState.KILLED;
        }
        if (getBatchJob() == null) {
            return ExecutionState.READY;
        }
        return getBatchJob().getState();
    }

    private void setBatchJob(IBatchJob batchJob) {
        this.batchJob = batchJob;
    }

    @Override
    public void update() throws InterruptedException {
        try {
            ExecutionState state;
            try {
                state = updateAndGetState();
            } catch (TimeoutException e) {
                Logger.getLogger(BatchExecutionJob.class.getName()).log(Level.WARNING, "Error in job update", e);
                state = getState();
            }
            
            Logger.getLogger(BatchExecutionJob.class.getName()).log(Level.INFO, "State " + state);

            switch (state) {
                case READY:
                    trySubmit();
                    break;
                case SUBMITED:
                case RUNNING:
                    break;
                case KILLED:
                case FAILED:
                    break;
                case DONE:
                    tryFinalise();
                    break;
            }
        } catch (InternalProcessingError e) {
            tryKill();
            Logger.getLogger(BatchExecutionJob.class.getName()).log(Level.WARNING, "Error in job update", e);
        } catch (UserBadDataError e) {
            tryKill();
            Logger.getLogger(BatchExecutionJob.class.getName()).log(Level.WARNING, "Error in job update", e);
        }

    }

    private void stopUpdate() {
        future.stopUpdate();
    }

    private void tryFinalise() throws InternalProcessingError, UserBadDataError {
        if (finalizeExecution == null) {
            finalizeExecution = Activator.getBackgroundExecutor().createBackgroundExecution(getGetResult());
        }
        try {
            if (getFinalizeExecutionOperation().isSucessFullStartIfNecessaryExceptionIfFailed(ExecutorType.DOWNLOAD)) {
                kill();
            }
        } catch (ExecutionException ex) {
            throw new InternalProcessingError(ex);
        } catch (InterruptedException ex) {
            throw new InternalProcessingError(ex);
        }
    }

    private GetResultFromEnvironment getGetResult() {
        if (getResult == null) {
            IJobStatisticCategory jobStatisticCategory = getEnvironment().getJobRegistries().getJobStatisticCategory(this);
            IExecutionContext executionContext = getEnvironment().getJobRegistries().getExecutionContext(this);
            getResult = new GetResultFromEnvironment(getInitStorage().getCommunicationStorage(), getInitStorage().getOutputFile(), getJob(), getEnvironment(), jobStatisticCategory, executionContext, getBatchJob().getLastStatusChangeInterval());
        }
        return getResult;
    }

    private IBackgroundExecution getFinalizeExecutionOperation() {
        return finalizeExecution;
    }

    private CopyToEnvironment getInitStorage() {
        return initStorage;
    }

    private void trySubmit() throws InternalProcessingError, UserBadDataError, InterruptedException {
        if (initStorageExec == null) {
            initStorageExec = Activator.getBackgroundExecutor().createBackgroundExecution(getInitStorage());
            initStorageExec.start(ExecutorType.UPLOAD);
        }
        try {
            if (initStorageExec.isSucessFullStartIfNecessaryExceptionIfFailed(ExecutorType.UPLOAD)) {
                Duo<JS, IAccessToken> js = getEnvironment().getAJobService();
                try {
                    IBatchJob bj = js.getLeft().createBatchJob(getInitStorage().getInputFile(), getInitStorage().getOutputFile(), getInitStorage().getRuntime());
                    bj.submit(js.getRight());
                    setBatchJob(bj);
                } catch (InternalProcessingError e) {
                    Logger.getLogger(BatchExecutionJob.class.getName()).log(Level.WARNING, "Error durring job submission.", e);
                } finally {
                    Activator.getBatchRessourceControl().releaseToken(js.getLeft().getDescription(), js.getRight());
                }
            }
        } catch (ExecutionException ex) {
            throw new InternalProcessingError(ex);
        }
    }

    @Override
    public long getUpdateInterval() {
        return updateInterval;
    }

    private void clean() throws InterruptedException, InternalProcessingError {
        if (getInitStorage().isInitialized()) {
            try {
                getInitStorage().getCommunicationDir().remove(true);
            } catch (IOException ex) {
                throw new InternalProcessingError(ex);
            }
        }
    }

    void tryKill() throws InterruptedException {
        try {
            kill();
        } catch (InternalProcessingError ex) {
            Logger.getLogger(BatchExecutionJob.class.getName()).log(Level.SEVERE, "Error durring job kill.", ex);
        } catch (UserBadDataError ex) {
            Logger.getLogger(BatchExecutionJob.class.getName()).log(Level.SEVERE, "Error durring job kill.", ex);
        }
    }

    @Override
    public void kill() throws InterruptedException, UserBadDataError, InternalProcessingError {
        //stopUpdate();
        boolean performKill;

        Lock lock = getKillLock();
        lock.lock();

        try {
            performKill = !killed;
            killed = true;
        } finally {
            lock.unlock();
        }

        if (performKill) {
            try {
                try {
                    if (initStorageExec != null) {
                        initStorageExec.interrupt();
                    }
                    if (finalizeExecution != null) {
                        finalizeExecution.interrupt();
                    }
                    clean();
                } finally {
                    if (getBatchJob() != null) {
                        getBatchJob().kill();
                    }
                }
            } finally {
                stopUpdate();
            }
        }
    }

    private synchronized Lock getKillLock() {
        if(killLock == null) killLock = new ReentrantLock();
        return killLock;
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();

        ret.append("Job ");
        ret.append(getState().getLabel());

        return ret.toString();
    }
}
