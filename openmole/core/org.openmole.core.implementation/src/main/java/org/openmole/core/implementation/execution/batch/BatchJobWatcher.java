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

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.misc.executorservice.ExecutorType;
import org.openmole.core.model.execution.batch.IBatchEnvironment;
import org.openmole.core.model.execution.batch.IBatchExecutionJob;
import org.openmole.core.model.execution.IExecutionJobRegistries;
import org.openmole.core.model.execution.IJobStatisticCategory;
import org.openmole.core.model.job.IJob;
import org.openmole.core.model.mole.IExecutionContext;
import org.openmole.commons.tools.structure.Trio;
import org.openmole.core.implementation.internal.Activator;
import org.openmole.misc.updater.IUpdatable;
import org.openmole.misc.workspace.ConfigurationLocation;

public class BatchJobWatcher implements IUpdatable {

    final static ConfigurationLocation CheckInterval = new ConfigurationLocation(BatchJobWatcher.class.getName(), "CheckInterval");

    static {
        Activator.getWorkspace().addToConfigurations(CheckInterval, "PT2M");
    }

    final long interval;
    final IBatchEnvironment<?> watchedEnv;

    public BatchJobWatcher(IBatchEnvironment<?> watchedEnv) throws InternalProcessingError {
        super();
        this.interval = Activator.getWorkspace().getPreferenceAsDurationInMs(CheckInterval);
        this.watchedEnv = watchedEnv;
    }

    @Override
    public long getUpdateInterval() {
        return interval;
    }

    @Override
    public void update() throws InterruptedException {
        IExecutionJobRegistries<IBatchExecutionJob> registries = watchedEnv.getJobRegistries();

        List<Trio<IExecutionContext, IJobStatisticCategory, IJob>> jobGroupsToRemove = new LinkedList<Trio<IExecutionContext, IJobStatisticCategory, IJob>>();

        // Map<ExecutionState, Integer> accounting = new HashMap<ExecutionState, Integer>();
        synchronized (registries) {
            for (Trio<IExecutionContext, IJobStatisticCategory, IJob> jobInfo : registries.getAllJobs()) {

                IJob job = jobInfo.getRight();
                IExecutionContext executionContext = jobInfo.getLeft();
                IJobStatisticCategory capsule = jobInfo.getCenter();

                if (job.allMoleJobsFinished()) {
                    for (final IBatchExecutionJob ej : registries.getAllExecutionJobs(executionContext, capsule, job)) {
                        org.openmole.core.implementation.internal.Activator.getExecutorService().getExecutorService(ExecutorType.KILL_REMOVE).submit(new Runnable() {

                            @Override
                            public void run() {
                                try {
                                    ej.kill();
                                } catch (InternalProcessingError ex) {
                                    Logger.getLogger(BatchJobWatcher.class.getName()).log(Level.INFO, "Error durring job kill.", ex);
                                } catch (UserBadDataError ex) {
                                    Logger.getLogger(BatchJobWatcher.class.getName()).log(Level.INFO, "Error durring job kill.", ex);
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(BatchJobWatcher.class.getName()).log(Level.INFO, "Error durring job kill.", ex);
                                }

                            }
                        });
                    }

                    jobGroupsToRemove.add(new Trio<IExecutionContext, IJobStatisticCategory, IJob>(executionContext, capsule, job));
                } else {

                    List<IBatchExecutionJob<?>> executionJobsToRemove = new LinkedList<IBatchExecutionJob<?>>();

                    for (final IBatchExecutionJob<?> ej : registries.getAllExecutionJobs(executionContext, capsule, job)) {
                        switch (ej.getState()) {
                            case FAILED:
                                org.openmole.core.implementation.internal.Activator.getExecutorService().getExecutorService(ExecutorType.KILL_REMOVE).submit(new Runnable() {

                                    @Override
                                    public void run() {
                                        try {
                                            ej.kill();
                                        } catch (InternalProcessingError ex) {
                                            Logger.getLogger(BatchJobWatcher.class.getName()).log(Level.INFO, "Error durring job kill.", ex);
                                        } catch (UserBadDataError ex) {
                                            Logger.getLogger(BatchJobWatcher.class.getName()).log(Level.INFO, "Error durring job kill.", ex);
                                        } catch (InterruptedException ex) {
                                            Logger.getLogger(BatchJobWatcher.class.getName()).log(Level.INFO, "Error durring job kill.", ex);
                                        }
                                    }
                                });
                            case KILLED:
                                executionJobsToRemove.add(ej);
                        }
                    }

                    for (IBatchExecutionJob<?> ej : executionJobsToRemove) {
                        registries.remove(executionContext, capsule, ej);
                    }

                    if (registries.getNbExecutionJobs(executionContext, capsule, job) == 0) {
                        try {
                            watchedEnv.submit(job, jobInfo.getLeft(), jobInfo.getCenter());
                        } catch (InternalProcessingError e) {
                            Logger.getLogger(BatchJobWatcher.class.getName()).log(Level.SEVERE, "Submission of job failed, job isn't being executed.", e);
                        } catch (UserBadDataError e) {
                            Logger.getLogger(BatchJobWatcher.class.getName()).log(Level.SEVERE, "Submission of job failed, job isn't being executed.", e);
                        }
                    }
                }


            }

            for (Trio<IExecutionContext, IJobStatisticCategory, IJob> j : jobGroupsToRemove) {
                registries.remove(j.getLeft(), j.getCenter(), j.getRight());
            }

        }

    }
}
