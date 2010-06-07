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
package org.openmole.core.implementation.execution.local;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.openmole.core.model.execution.ExecutionState;
import org.openmole.core.model.execution.IJobStatisticCategory;
import org.openmole.core.model.execution.batch.SampleType;
import org.openmole.core.model.job.IJob;
import org.openmole.core.model.job.IMoleJob;
import org.openmole.core.model.mole.IExecutionContext;
import org.openmole.core.model.task.IMoleTask;
import org.openmole.commons.tools.structure.Trio;
import org.openmole.core.model.job.State;

public class LocalExecuter implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(LocalExecuter.class.getName());
    LocalExecutionEnvironment localEnvironment;
    boolean stop = false;

    public LocalExecuter(LocalExecutionEnvironment localEnvironment) {
        super();
        this.localEnvironment = localEnvironment;
    }

    @Override
    public void run() {

        while (!stop) {
            try {
                Trio<IExecutionContext, IJobStatisticCategory, LocalExecutionJob> trio = localEnvironment.takeNextjob();
                LocalExecutionJob executionJob = trio.getRight();
                IJob job = executionJob.getJob();

                IExecutionContext executionContext = trio.getLeft();
                IJobStatisticCategory taskCapsule = trio.getCenter();

                try {
                    executionJob.setState(ExecutionState.RUNNING);
                    Long running = System.currentTimeMillis();
                    executionJob.getEnvironment().sample(SampleType.WAITING, running - executionJob.getCreationTime(), executionContext, taskCapsule);

                    for (IMoleJob moleJob : job.getMoleJobs()) {
                        if (moleJob.getState() != State.CANCELED) {
                            LOGGER.finer("New job group taken for execution: " + moleJob);

                            if (IMoleTask.class.isAssignableFrom(moleJob.getTask().getClass())) {
                                jobGoneIdle();
                            }
                            moleJob.perform(executionContext);
                            moleJob.finished(moleJob.getContext());
                            LOGGER.finer("End of job group execution: " + moleJob);
                        }
                    }
                    executionJob.setState(ExecutionState.DONE);
                    executionJob.getEnvironment().sample(SampleType.RUNNING, System.currentTimeMillis() - running, executionContext, taskCapsule);
                } finally {
                    localEnvironment.getJobRegistries().remove(executionContext, taskCapsule, job);
                }
            } catch (InterruptedException e) {
                if (!stop) {
                    LOGGER.log(Level.WARNING, "Interrupted despite stop is false.", e);
                }
            } catch (Throwable e) {
                LOGGER.log(Level.SEVERE, null, e);
            }
        }

    }

    protected boolean isStop() {
        return stop;
    }

    protected void setStop(boolean stop) {
        this.stop = stop;
    }

    public void jobGoneIdle() {
        localEnvironment.addExecuters(1);
        setStop(true);
    }
}
