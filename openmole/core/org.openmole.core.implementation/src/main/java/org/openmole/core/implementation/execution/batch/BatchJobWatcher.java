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
import org.openmole.core.model.execution.batch.IBatchEnvironment;
import org.openmole.core.model.execution.batch.IBatchExecutionJob;
import org.openmole.core.model.job.IJob;
import org.openmole.core.implementation.internal.Activator;
import org.openmole.core.model.execution.IExecutionJobRegistry;
import org.openmole.misc.updater.IUpdatable;
import org.openmole.misc.workspace.ConfigurationLocation;

public class BatchJobWatcher implements IUpdatable {

    final static ConfigurationLocation CheckInterval = new ConfigurationLocation(BatchJobWatcher.class.getName(), "CheckInterval");

    static {
        Activator.getWorkspace().addToConfigurations(CheckInterval, "PT2M");
    }

    final IBatchEnvironment<?> watchedEnv;

    public BatchJobWatcher(IBatchEnvironment<?> watchedEnv) throws InternalProcessingError {
        super();
        this.watchedEnv = watchedEnv;
    }

    @Override
    public boolean update() throws InterruptedException {
        IExecutionJobRegistry<IBatchExecutionJob> registry = watchedEnv.getJobRegistry();
        List<IJob> jobGroupsToRemove = new LinkedList<IJob>();
        
        synchronized (registry) {
            for (IJob job : registry.getAllJobs()) {
         
                if (job.allMoleJobsFinished()) {
                    for (final IBatchExecutionJob ej : registry.getExecutionJobsFor(job)) {
                        ej.kill();
                    }

                    jobGroupsToRemove.add(job);
                } else {

                    List<IBatchExecutionJob<?>> executionJobsToRemove = new LinkedList<IBatchExecutionJob<?>>();

                    for (final IBatchExecutionJob<?> ej : registry.getExecutionJobsFor(job)) {
                        switch (ej.getState()) {
                            case KILLED:
                                executionJobsToRemove.add(ej);
                                break;
                        }
                    }

                    for (IBatchExecutionJob<?> ej : executionJobsToRemove) {
                        registry.remove(ej);
                    }

                    if (registry.getNbExecutionJobsForJob(job) == 0) {
                        try {
                            watchedEnv.submit(job);
                        } catch (InternalProcessingError e) {
                            Logger.getLogger(BatchJobWatcher.class.getName()).log(Level.SEVERE, "Submission of job failed, job isn't being executed.", e);
                        } catch (UserBadDataError e) {
                            Logger.getLogger(BatchJobWatcher.class.getName()).log(Level.SEVERE, "Submission of job failed, job isn't being executed.", e);
                        }
                    }
                }
            }

            for (IJob j : jobGroupsToRemove) {
                registry.removeJob(j);
            }
        }
        return true;
    }
}
