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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
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
import org.openmole.core.file.URIFileCleaner;
import org.openmole.misc.updater.IUpdatable;
import org.openmole.misc.updater.IUpdatableFuture;
import org.openmole.misc.workspace.ConfigurationLocation;
import org.openmole.misc.backgroundexecutor.IBackgroundExecution;
import org.openmole.core.implementation.execution.ExecutionJob;
import org.openmole.core.model.job.IJob;

public class BatchExecutionJob<JS extends IBatchJobService> extends ExecutionJob<BatchEnvironment<JS>> implements IBatchExecutionJob<BatchEnvironment<JS>>, IUpdatable {

    final static String configurationGroup = BatchExecutionJob.class.getSimpleName();
    final static ConfigurationLocation UpdateInterval = new ConfigurationLocation(configurationGroup, "UpdateInterval");

    static {
        Activator.getWorkspace().addToConfigurations(UpdateInterval, "PT2M");
    }
    IUpdatableFuture future;
    IBatchJob batchJob;
    final AtomicBoolean killed = new AtomicBoolean(false);
    CopyToEnvironment.Result copyToEnvironmentResult = null;
    transient IBackgroundExecution<CopyToEnvironment.Result> copyToEnvironmentExec;
    transient IBackgroundExecution finalizeExecution;

    public BatchExecutionJob(BatchEnvironment<JS> executionEnvironment, IJob job) {
        super(executionEnvironment, job);
    }

    public void setFuture(IUpdatableFuture future) {
        this.future = future;
    }

    @Override
    public IBatchJob getBatchJob() {
        return batchJob;
    }

    private ExecutionState updateAndGetState() throws InternalProcessingError, UserBadDataError, InterruptedException {
        if (getBatchJob() == null) {
            return ExecutionState.READY;
        }
        if (killed.get()) {
            return ExecutionState.KILLED;
        }

        ExecutionState oldState = getBatchJob().getState();

        if (!oldState.isFinal()) {
            ExecutionState newState = getBatchJob().getUpdatedState();

            if (oldState == ExecutionState.SUBMITED && newState == ExecutionState.RUNNING) {
                getEnvironment().sample(SampleType.WAITING, getBatchJob().getLastStatusDurration(), getJob());
            }
        }

        return getState();
    }

    @Override
    public ExecutionState getState() {
        if (killed.get()) {
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
            ExecutionState state = updateAndGetState();

            switch (state) {
                case READY:
                    trySubmit();
                    break;
                case SUBMITED:
                case RUNNING:
                    break;
                case KILLED:
                    break;
                case FAILED:
                    break;
                case DONE:
                    tryFinalise();
                    break;
            }
        } catch (InternalProcessingError e) {
            kill();
            Logger.getLogger(BatchExecutionJob.class.getName()).log(Level.WARNING, "Error in job update", e);
        } catch (UserBadDataError e) {
            kill();
            Logger.getLogger(BatchExecutionJob.class.getName()).log(Level.WARNING, "Error in job update", e);
        }

    }

    private void stopUpdate() {
        future.stopUpdate();
    }

    private void tryFinalise() throws InternalProcessingError, UserBadDataError {
        if (finalizeExecution == null) {
            finalizeExecution = Activator.getBackgroundExecutor().createBackgroundExecution(new GetResultFromEnvironment(copyToEnvironmentResult.communicationStorage.getDescription(), copyToEnvironmentResult.outputFile, getJob(), getEnvironment(), getBatchJob().getLastStatusDurration()));
        }
        try {
            if (finalizeExecution.isSucessFullStartIfNecessaryExceptionIfFailed(ExecutorType.DOWNLOAD)) {
                finalizeExecution = null;
                kill();
            }
        } catch (ExecutionException ex) {
            throw new InternalProcessingError(ex);
        }
    }

    private void trySubmit() throws InternalProcessingError, UserBadDataError, InterruptedException {
        if (copyToEnvironmentExec == null) {
            copyToEnvironmentExec = Activator.getBackgroundExecutor().createBackgroundExecution(new CopyToEnvironment(getEnvironment(), getJob()));
        }

        try {
            if (copyToEnvironmentExec.isSucessFullStartIfNecessaryExceptionIfFailed(ExecutorType.UPLOAD)) {

                Duo<JS, IAccessToken> js = getEnvironment().getAJobService();
                try {
                    IBatchJob bj = js.getLeft().createBatchJob(copyToEnvironmentResult.inputFile, copyToEnvironmentResult.outputFile, copyToEnvironmentResult.runtime);
                    bj.submit(js.getRight());
                    setBatchJob(bj);

                    copyToEnvironmentResult = copyToEnvironmentExec.getResult();
                    copyToEnvironmentExec = null;
                } catch (InternalProcessingError e) {
                    Logger.getLogger(BatchExecutionJob.class.getName()).log(Level.FINE, "Error durring job submission.", e);
                } finally {
                    Activator.getBatchRessourceControl().getController(js.getLeft().getDescription()).getUsageControl().releaseToken(js.getRight());
                }
            }
        } catch (ExecutionException ex) {
            throw new InternalProcessingError(ex);
        }

    }

    private void clean() {
        if (copyToEnvironmentResult != null) {
            Activator.getExecutorService().getExecutorService(ExecutorType.REMOVE).submit(new URIFileCleaner(copyToEnvironmentResult.communicationDir, true));
        }
    }

    @Override
    public void kill() {
        if (!killed.getAndSet(true)) {
            try {
                try {
                    if (copyToEnvironmentExec != null) {
                        copyToEnvironmentExec.interrupt();
                    }
                    if (finalizeExecution != null) {
                        finalizeExecution.interrupt();
                    }
                    clean();
                } finally {
                    IBatchJob bj = getBatchJob();
                    if (bj != null) {
                        Activator.getExecutorService().getExecutorService(ExecutorType.KILL).submit(new BatchJobKiller(bj));
                    }
                }
            } finally {
                stopUpdate();
            }
        }
    }
}
