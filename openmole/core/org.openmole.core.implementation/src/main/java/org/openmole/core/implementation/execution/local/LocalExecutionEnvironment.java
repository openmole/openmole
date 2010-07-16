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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;
import org.openmole.core.model.execution.IExecutionJob;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.misc.executorservice.ExecutorType;
import org.openmole.core.implementation.execution.Environment;
import org.openmole.core.implementation.internal.Activator;
import org.openmole.core.implementation.job.Job;
import org.openmole.core.model.execution.ExecutionState;
import org.openmole.core.model.job.IJob;
import org.openmole.core.model.job.IMoleJob;
import org.openmole.misc.workspace.ConfigurationLocation;

public class LocalExecutionEnvironment extends Environment<IExecutionJob> {

    final static String ConfigGroup = LocalExecutionEnvironment.class.getSimpleName();
    final static ConfigurationLocation DefaultNumberOfThreads = new ConfigurationLocation(ConfigGroup, "Time");

    static {
        Activator.getWorkspace().addToConfigurations(DefaultNumberOfThreads, "1");
    }

    private static final Logger LOGGER = Logger.getLogger(LocalExecutionEnvironment.class.getName());
    private static LocalExecutionEnvironment instance;

    BlockingQueue<LocalExecutionJob> jobs = new LinkedBlockingQueue<LocalExecutionJob>();
    final Map<LocalExecuter, Future<?>> executers = new HashMap<LocalExecuter, Future<?>>();
    int nbThread;

    private LocalExecutionEnvironment() throws InternalProcessingError {
        super();
        this.nbThread = Activator.getWorkspace().getPreferenceAsInt(DefaultNumberOfThreads);
        addExecuters(nbThread);
    }

    void addExecuters(int nbExecuters) {
        for (int i = 0; i < nbExecuters; i++) {
            LocalExecuter executer = new LocalExecuter(this);
            synchronized (executers) {
                Future<?> f = Activator.getExecutorService().getExecutorService(ExecutorType.OWN).submit(executer);
                executers.put(executer, f);
            }
        }
    }

    public int getNbThread() {
        return nbThread;
    }

    public synchronized void setNbThread(int newNbThread) {
        if (nbThread == newNbThread) {
            return;

        }
        if (newNbThread > nbThread) {
            addExecuters(newNbThread - nbThread);
        } else {
            int toStop = nbThread - newNbThread;
            synchronized (executers) {
                Iterator<LocalExecuter> it = executers.keySet().iterator();

                while (it.hasNext() && toStop > 0) {
                    LocalExecuter exe = it.next();
                    if (!exe.isStop()) {
                        exe.setStop(true);
                        it.remove();
                        toStop--;
                    }
                }
            }
        }

        nbThread = newNbThread;
    }



    @Override
    public void submit(IJob job) throws InternalProcessingError, UserBadDataError {
         LocalExecutionJob ejob = new LocalExecutionJob(this, job);
         submit(ejob);
         getJobRegistry().register(ejob);
    }

    public void submit(IMoleJob moleJob) throws InternalProcessingError, UserBadDataError {
        Job job = new Job();
        job.addMoleJob(moleJob);
        submit(job);
    }

    public void submit(LocalExecutionJob ejob) {
        ejob.setState(ExecutionState.SUBMITED);
        LOGGER.finer("New job submitted: " + ejob.getJob());
        jobs.add(ejob);
    }

    LocalExecutionJob takeNextjob() throws InterruptedException {
        return jobs.take();
    }

    public synchronized static LocalExecutionEnvironment getInstance() throws InternalProcessingError {
        if(instance == null) {
            instance = new LocalExecutionEnvironment();
        }
        return instance;
    }

   
}
