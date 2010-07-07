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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.openmole.commons.aspect.eventdispatcher.BeforeObjectModified;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.model.execution.IEnvironment;
import org.openmole.core.model.job.IMoleJob;
import org.openmole.core.model.capsule.IGenericTaskCapsule;
import org.openmole.commons.tools.structure.Trio;
import org.openmole.core.implementation.internal.Activator;
import org.openmole.core.implementation.job.Context;
import org.openmole.core.implementation.job.Job;
import org.openmole.core.implementation.job.MoleJob;
import org.openmole.core.implementation.job.MoleJobId;
import org.openmole.core.model.execution.IMoleJobCategory;
import org.openmole.core.model.execution.IMoleJobGroupingStrategy;
import org.openmole.core.model.mole.ISubMoleExecution;
import org.openmole.core.model.job.IContext;
import org.openmole.core.model.job.IJob;
import org.openmole.core.model.job.IMoleJobId;
import org.openmole.core.model.job.ITicket;
import org.openmole.core.model.mole.IMoleExecution;
import org.openmole.core.model.mole.ILocalCommunication;
import org.openmole.commons.aspect.eventdispatcher.IObjectChangedSynchronousListener;
import org.openmole.commons.tools.structure.Duo;
import org.openmole.commons.tools.structure.Priority;
import org.openmole.core.implementation.execution.JobRegistry;
import org.openmole.core.implementation.execution.local.LocalExecutionEnvironment;
import org.openmole.core.model.mole.IEnvironmentSelection;
import org.openmole.core.model.mole.IMole;
import org.openmole.core.model.mole.IMoleJobGrouping;

public class MoleExecution implements IMoleExecution {

    class MoleExecutionAdapterForMoleJobOutputTransitionPerformed implements IObjectChangedSynchronousListener<IMoleJob> {

        @Override
        public void objectChanged(IMoleJob job) throws InternalProcessingError, UserBadDataError {
            jobOutputTransitionsPerformed(job);
        }

    }

    class MoleExecutionAdapterForMoleJob implements IObjectChangedSynchronousListener<IMoleJob> {

        @Override
        public void objectChanged(IMoleJob job) throws InternalProcessingError, UserBadDataError {
            switch (job.getState()) {
                case FAILED:
                    jobFailed(job);
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
    
    
    
    final IMole mole;
    final IMoleJobGrouping moleJobGrouping;

    final BlockingQueue<Duo<IJob, IEnvironment>> jobs = new LinkedBlockingQueue<Duo<IJob, IEnvironment>>();
    final Map<IMoleJob, ISubMoleExecution> inProgress = Collections.synchronizedMap(new TreeMap<IMoleJob, ISubMoleExecution>());

    final AtomicLong ticketNumber = new AtomicLong();
    final AtomicLong currentJobId = new AtomicLong();

    final ILocalCommunication localCommunication;
    final IEnvironmentSelection environmentSelectionStrategy;
    final IEnvironment defaultEnvironment;
    
    final BidiMap<Trio<ISubMoleExecution, IGenericTaskCapsule, IMoleJobCategory>, Job> categorizer = new DualHashBidiMap<Trio<ISubMoleExecution, IGenericTaskCapsule, IMoleJobCategory>, Job>();
    final MultiMap<ISubMoleExecution, Job> jobsGrouping = new MultiHashMap<ISubMoleExecution, Job>();

    final MoleExecutionAdapterForMoleJob moleExecutionAdapterForMoleJob = new MoleExecutionAdapterForMoleJob();
    final MoleExecutionAdapterForSubMoleExecution moleExecutionAdapterForSubMoleExecution = new MoleExecutionAdapterForSubMoleExecution();
    final MoleExecutionAdapterForMoleJobOutputTransitionPerformed moleJobOutputTransitionPerformed = new MoleExecutionAdapterForMoleJobOutputTransitionPerformed();

    transient Thread submiter;

    public MoleExecution(IMole mole) throws InternalProcessingError, UserBadDataError {
        this(mole, new Context(), FixedEnvironmentSelection.EMPTY_SELECTION, MoleJobGrouping.EMPTY_GROUPING);
    }
    
    public MoleExecution(IMole mole, IEnvironmentSelection environmentSelectionStrategy) throws InternalProcessingError, UserBadDataError {
        this(mole, new Context(), environmentSelectionStrategy, MoleJobGrouping.EMPTY_GROUPING);
    }

    public MoleExecution(IMole mole, IEnvironmentSelection environmentSelectionStrategy, IMoleJobGrouping moleJobGrouping) throws InternalProcessingError, UserBadDataError {
        this(mole, new Context(), environmentSelectionStrategy, moleJobGrouping);
    }
    
    public MoleExecution(IMole mole, IContext context) throws InternalProcessingError, UserBadDataError {
        this(mole, context, FixedEnvironmentSelection.EMPTY_SELECTION, MoleJobGrouping.EMPTY_GROUPING);
    }
    
    public MoleExecution(IMole mole, IContext context, IEnvironmentSelection environmentSelectionStrategy, IMoleJobGrouping moleJobGrouping) throws InternalProcessingError, UserBadDataError {
        this.mole = mole;
        this.moleJobGrouping = moleJobGrouping;
        this.localCommunication = new LocalCommunication();
        this.environmentSelectionStrategy = environmentSelectionStrategy;
        this.defaultEnvironment = LocalExecutionEnvironment.getInstance();

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
   
    public synchronized void submit(IMoleJob moleJob, IGenericTaskCapsule capsule, ISubMoleExecution subMole) throws InternalProcessingError, UserBadDataError {
        Activator.getEventDispatcher().objectChanged(this, oneJobSubmitted, new Object[]{moleJob});

        ExecutionInfoRegistry.GetInstance().register(moleJob, this);
        Activator.getEventDispatcher().registerListener(moleJob, Priority.HIGH.getValue(), moleExecutionAdapterForMoleJob, MoleJob.StateChanged);
        Activator.getEventDispatcher().registerListener(moleJob, Priority.NORMAL.getValue(), moleJobOutputTransitionPerformed, MoleJob.TransitionPerformed);

        inProgress.put(moleJob, subMole);
        subMole.incNbJobInProgress();

        IMoleJobGroupingStrategy strategy = moleJobGrouping.getMoleJobGroupingStrategy(capsule);

        if (strategy != null) {
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
            subMole.incNbJobWaitingInGroup();
        } else {
            Job job = new Job();
            job.addMoleJob(moleJob);
            submit(job, capsule);
        }

        LOGGER.log(Level.FINER, "A new job has been successfully submitted:{0}", moleJob);
    }

    private void submit(Job job, IGenericTaskCapsule capsule) {
        JobRegistry.getInstance().register(job, this);
        IEnvironment environment = environmentSelectionStrategy.selectEnvironment(capsule);
        environment = environment!=null?environment:defaultEnvironment;
        jobs.add(new Duo<IJob, IEnvironment>(job, environment));
    }

    synchronized void submitGroups(ISubMoleExecution subMoleExecution) {
        Iterable<Job> jobs = jobsGrouping.remove(subMoleExecution);

        LOGGER.finer("Submit a group");
        for (Job job : jobs) {
            Trio<ISubMoleExecution, IGenericTaskCapsule, IMoleJobCategory> info = categorizer.removeValue(job);
            subMoleExecution.decNbJobWaitingInGroup(job.size());
            submit(job, info.getCenter());
        }
    }


    class Submiter implements Runnable {

        boolean stop = false;

        @Override
        public void run() {
            while (!stop) {
                Duo<IJob, IEnvironment> p;
                try {
                    p = jobs.take();
                } catch (InterruptedException e) {
                    LOGGER.log(Level.FINE, "Scheduling interrupted", e);
                    return;
                }
                LOGGER.log(Level.FINER, "New job taken:{0}", p.getLeft());
                try {
                    p.getRight().submit(p.getLeft());
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
    @BeforeObjectModified(type=starting)
    public void start() throws InternalProcessingError, UserBadDataError {
        if (getSubmiter().getState().equals(Thread.State.NEW)) {
            getSubmiter().start();
        } else {
            LOGGER.warning("This MOLE execution has allready been started, this call has no effect.");
        }
    }

    @Override
    public synchronized void cancel() throws InternalProcessingError, UserBadDataError {
        getSubmiter().interrupt();

        synchronized(inProgress) {
            for (IMoleJob moleJob : inProgress.keySet()) {
                moleJob.cancel();
            }
            inProgress.clear();
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

  

    @Override
    public  Iterable<IMoleJob> getAllMoleJobs() {
        Collection<IMoleJob> ret = new LinkedList<IMoleJob>();

        synchronized(inProgress) {
            ret.addAll(inProgress.keySet());
        }

        return ret;
    }

    @Override
    public void waitUntilEnded() throws InterruptedException {
        getSubmiter().join();
    }

    private void jobFailed(IMoleJob job) throws InternalProcessingError, UserBadDataError {
        jobOutputTransitionsPerformed(job);
    }

    private synchronized void jobOutputTransitionsPerformed(IMoleJob job) throws InternalProcessingError, UserBadDataError {
        Activator.getEventDispatcher().objectChanged(this, oneJobFinished, new IMoleJob[]{job});

        ISubMoleExecution subMole = inProgress.get(job);

        subMole.decNbJobInProgress();

        if (subMole.getNbJobInProgess() == 0) {
            Object[] args = {job};
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
    public IMole getMole() {
        return mole;
    }

    @Override
    public  ITicket createRootTicket() {
        return new Ticket(UUID.randomUUID().toString(), ticketNumber.getAndIncrement());
    }
    

    @Override
    public ITicket nextTicket(ITicket parent) {
        return new Ticket(ticketNumber.getAndIncrement(), parent);
    }

    @Override
    public ILocalCommunication getLocalCommunication() {
        return localCommunication;
    }

    @Override
    public IMoleJobId nextJobId() {
        return new MoleJobId(currentJobId.getAndIncrement());
    }

    @Override
    public ISubMoleExecution getSubMoleExecution(IMoleJob job) {
        return inProgress.get(job);
    }

}
