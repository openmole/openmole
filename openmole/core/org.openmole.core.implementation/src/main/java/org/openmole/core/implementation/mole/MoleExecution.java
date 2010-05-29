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
package org.openmole.core.implementation.mole;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.openmole.core.implementation.data.Prototype;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.workflow.model.execution.IEnvironment;
import org.openmole.core.workflow.model.job.IMoleJob;
import org.openmole.core.workflow.model.capsule.IGenericTaskCapsule;
import org.openmole.commons.tools.structure.Trio;
import org.openmole.core.implementation.internal.Activator;
import org.openmole.core.implementation.job.Context;
import org.openmole.core.implementation.job.Job;
import org.openmole.core.implementation.job.MoleJob;
import org.openmole.core.implementation.job.MoleJobId;
import org.openmole.core.implementation.task.GenericTask;
import org.openmole.core.workflow.model.data.IPrototype;
import org.openmole.core.workflow.model.execution.IJobStatisticCategory;
import org.openmole.core.workflow.model.execution.IMoleJobCategory;
import org.openmole.core.workflow.model.execution.IMoleJobGroupingStrategy;
import org.openmole.core.workflow.model.mole.ISubMoleExecution;
import org.openmole.core.workflow.model.job.IContext;
import org.openmole.core.workflow.model.job.IJob;
import org.openmole.core.workflow.model.job.IMoleJobId;
import org.openmole.core.workflow.model.job.ITicket;
import org.openmole.core.workflow.model.mole.IExecutionContext;
import org.openmole.core.workflow.model.mole.IMoleExecution;
import org.openmole.core.workflow.model.mole.ILocalCommunication;
import org.openmole.commons.aspect.eventdispatcher.IObjectChangedSynchronousListener;
import org.openmole.commons.tools.structure.Priority;

public class MoleExecution implements IMoleExecution {

    final static public IPrototype<Collection> Exceptions = new Prototype<Collection>("Exceptions", Collection.class);

    class MoleExecutionAdapterForMoleJob implements IObjectChangedSynchronousListener<IMoleJob> {

        @Override
        public void objectChanged(IMoleJob job) throws InternalProcessingError, UserBadDataError {
            switch (job.getState()) {
                case FAILED:
                case TRANSITION_PERFORMED:
                    jobOutputTransitionsPerformed(job);
                    break;
                case COMPLETED:
                    jobFinished(job);
                    break;
            }
        }
    }

    class MoleExecutionAdapterForSubMoleExecution implements IObjectChangedSynchronousListener<ISubMoleExecution> {

        @Override
        public void objectChanged(ISubMoleExecution obj) throws InternalProcessingError, UserBadDataError {
            submitGroups(obj);
        }
    }
    private static final Logger LOGGER = Logger.getLogger(MoleExecution.class.getName());
    Mole mole;
    BlockingQueue<Trio<IJob, IEnvironment, IJobStatisticCategory>> jobs = new LinkedBlockingQueue<Trio<IJob, IEnvironment, IJobStatisticCategory>>();
    Map<IMoleJob, ISubMoleExecution> inProgress = Collections.synchronizedMap(new TreeMap<IMoleJob, ISubMoleExecution>());
    Long ticketNumber = 0L;
    Long currentJobId = 0L;
    ILocalCommunication localCommunication;
    IExecutionContext executionContext;
    BidiMap<Trio<ISubMoleExecution, IGenericTaskCapsule, IMoleJobCategory>, Job> categorizer = new DualHashBidiMap<Trio<ISubMoleExecution, IGenericTaskCapsule, IMoleJobCategory>, Job>();
    MultiMap<ISubMoleExecution, Job> jobsGrouping = new MultiHashMap<ISubMoleExecution, Job>();
    MoleExecutionAdapterForMoleJob moleExecutionAdapterForMoleJob = new MoleExecutionAdapterForMoleJob();
    final MoleExecutionAdapterForSubMoleExecution moleExecutionAdapterForSubMoleExecution = new MoleExecutionAdapterForSubMoleExecution();
    transient Thread submiter;

    public MoleExecution(Mole mole, IContext context, IExecutionContext executionContext) throws InternalProcessingError, UserBadDataError {
        this.mole = mole;
        this.localCommunication = new LocalCommunication();
        this.executionContext = executionContext;

        ITicket ticket = nextTicket(createRootTicket());

        if (context.isRoot()) {
            context = new Context(context);
        }

        IGenericTaskCapsule root = mole.getRoot();
        if (root == null) {
            throw new UserBadDataError("First task of workflow hasn't been set.");
        }

        submit(root, context, ticket, new SubMoleExecution());
    }

    @Override
    public synchronized void submit(IGenericTaskCapsule capsule, IContext context, ITicket ticket, ISubMoleExecution subMole) throws InternalProcessingError, UserBadDataError {
        IMoleJob job = capsule.toJob(context, ticket, nextJobId());
        submit(job, capsule, subMole);
    }

    @Override
    public synchronized void submit(IMoleJob moleJob, IGenericTaskCapsule capsule, ISubMoleExecution subMole) throws InternalProcessingError, UserBadDataError {
        ExecutionInfoRegistry.GetInstance().register(moleJob, this);
        Activator.getEventDispatcher().registerListener(moleJob, getLevel(), moleExecutionAdapterForMoleJob, MoleJob.stateChanged);

        inProgress.put(moleJob, subMole);
        subMole.incNbJobInProgress();

        IMoleJobGroupingStrategy strategy = mole.getMoleJobGroupingStrategy(capsule);

        //LOGGER.info("Job in mole execution");


        if (strategy != null) {
          //  LOGGER.info("Grouping job");

            IMoleJobCategory category = strategy.getCategory(moleJob.getContext());

            Trio<ISubMoleExecution, IGenericTaskCapsule, IMoleJobCategory> key = new Trio<ISubMoleExecution, IGenericTaskCapsule, IMoleJobCategory>(subMole, capsule, category);
            Job job = categorizer.get(key);
            if (job == null) {
                job = new Job();
                categorizer.put(key, job);
                jobsGrouping.put(subMole, job);

                if (!Activator.getEventDispatcher().isRegistred(subMole, moleExecutionAdapterForSubMoleExecution, ISubMoleExecution.allJobsWaitingInGroup)) {
                    Activator.getEventDispatcher().registerListener(subMole, Priority.NORMAL.getValue(), moleExecutionAdapterForSubMoleExecution, ISubMoleExecution.allJobsWaitingInGroup);
                }
            }

            job.addMoleJob(moleJob);

           // LOGGER.info("Inwating jobs");
            subMole.incNbJobWaitingInGroup();
        } else {
            Job job = new Job();
            job.addMoleJob(moleJob);
            submit(job, capsule);
        }

        LOGGER.finer("A new job has been successfully submitted:" + moleJob);
    }

    private void submit(Job job, IGenericTaskCapsule capsule) {
        IJobStatisticCategory jobStatisticCategory = getMole().getJobStatisticCategorizationStrategy().getCategory(job, capsule);
        IEnvironment environment = executionContext.getEnvironmentSelectionStrategy().selectEnvironment(capsule);

        jobs.add(new Trio<IJob, IEnvironment, IJobStatisticCategory>(job, environment, jobStatisticCategory));
    }

    synchronized void submitGroups(ISubMoleExecution subMoleExecution) {
        Iterable<Job> jobs = jobsGrouping.remove(subMoleExecution);

LOGGER.info("Submit a group");
        for (Job job : jobs) {
            Trio<ISubMoleExecution, IGenericTaskCapsule, IMoleJobCategory> info = categorizer.removeValue(job);
            subMoleExecution.decNbJobWaitingInGroup(job.getNbMoleJob());
            submit(job, info.getCenter());
        }
    }

    @Override
    public int getLevel() {
        return 0;
    }

    class Submiter implements Runnable {

        boolean stop = false;

        @Override
        public void run() {
            while (!stop) {
                Trio<IJob, IEnvironment, IJobStatisticCategory> p;
                try {
                    p = jobs.take();
                } catch (InterruptedException e) {
                    LOGGER.log(Level.FINE, "Scheduling interrupted", e);
                    return;
                }
                LOGGER.finer("New job taken:" + p.getLeft());
                try {
                    p.getCenter().submit(p.getLeft(), executionContext, p.getRight());
                } catch (UserBadDataError e) {
                    LOGGER.log(Level.SEVERE, "Error durring scheduling", e);
                } catch (InternalProcessingError e) {
                    LOGGER.log(Level.SEVERE, "Error durring scheduling", e);

                } catch (Throwable t) {
                    LOGGER.log(Level.SEVERE, "Error durring scheduling", t);
                }

            }
        }
    }

    @Override
    public void start() {
        if (getSubmiter().getState().equals(Thread.State.NEW)) {
            getSubmiter().start();
        } else {
            LOGGER.warning("This MOLE execution has allready been started, this call has no effect.");
        }
    }

    @Override
    public synchronized void cancel() throws InternalProcessingError, UserBadDataError {
        getSubmiter().interrupt();

        for (IMoleJob moleJob : getAllMoleJobsInternal()) {
            moleJob.cancel();
        }

    }

    private synchronized Thread getSubmiter() {
        if (submiter == null) {
            submiter = new Thread(new Submiter());
            submiter.setDaemon(true);
            submiter.setPriority(Thread.MAX_PRIORITY);
        }
        return submiter;
    }

    private Iterable<IMoleJob> getAllMoleJobsInternal() {
        return inProgress.keySet();
    }

    @Override
    public synchronized Iterable<IMoleJob> getAllMoleJobs() {
        Collection<IMoleJob> ret = new LinkedList<IMoleJob>();

        for (IMoleJob moleJob : getAllMoleJobsInternal()) {
            ret.add(moleJob);
        }

        return ret;
    }

    @Override
    public void waitUntilEnded() throws InterruptedException {
        getSubmiter().join();
    }


    /*public synchronized void jobFailed(IMoleJob job) throws InternalProcessingError, UserBadDataError {
        Activator.getEventDispatcher().objectChanged(this, oneJobJinished, new IMoleJob[]{job});
        ISubMoleExecution subMole = inProgress.get(job);
        subMole.decNbJobInProgress();


    }*/


    public synchronized void jobOutputTransitionsPerformed(IMoleJob job) throws InternalProcessingError, UserBadDataError {
        Activator.getEventDispatcher().objectChanged(this, oneJobJinished, new IMoleJob[]{job});

        ISubMoleExecution subMole = inProgress.get(job);

        subMole.decNbJobInProgress();

        if (subMole.getNbJobInProgess() == 0) {
            Object[] args = {job, executionContext};
            Activator.getEventDispatcher().objectChanged(subMole, ISubMoleExecution.finished, args);
        }

        inProgress.remove(job);

        if (isFinished()) {
            getSubmiter().interrupt();
            Object[] args = {job};
            Activator.getEventDispatcher().objectChanged(this, finished, args);
        }

    }

    @Override
    public boolean isFinished() {
        return inProgress.isEmpty();
    }

    @Override
    public Mole getMole() {
        return mole;
    }

    @Override
    public synchronized ITicket createRootTicket() {
        return new Ticket(UUID.randomUUID().toString(), ticketNumber++);
    }

    @Override
    public synchronized ITicket nextTicket(ITicket parent) {
        return new Ticket(ticketNumber++, parent);
    }

    @Override
    public ILocalCommunication getLocalCommunication() {
        return localCommunication;
    }

    @Override
    public synchronized IMoleJobId nextJobId() {
        return new MoleJobId(currentJobId++);
    }

    @Override
    public ISubMoleExecution getSubMoleExecution(IMoleJob job) {
        return inProgress.get(job);
    }

    void jobFinished(IMoleJob job) throws InternalProcessingError, UserBadDataError {
        if (job.getContext().contains(Exceptions)) {
            IContext rootCtx = job.getContext().getRoot();
            Collection<Throwable> exceptions;

            synchronized (rootCtx) {
                if (rootCtx.contains(Exceptions)) {
                    exceptions = rootCtx.getLocalValue(Exceptions);
                } else {
                    exceptions = Collections.synchronizedCollection(new ArrayList<Throwable>());
                    rootCtx.setValue(Exceptions, exceptions);
                }
            }

            exceptions.add(job.getContext().getLocalValue(GenericTask.Exception.getPrototype()));
        }

    }

    @Override
    public IExecutionContext getExecutionContext() {
        return executionContext;
    }
}
