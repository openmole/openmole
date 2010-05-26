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
package org.openmole.core.workflow.implementation.job;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import org.openmole.core.workflow.implementation.execution.Progress;
import org.openmole.commons.aspect.eventdispatcher.ObjectConstructed;
import org.openmole.commons.aspect.eventdispatcher.ObjectModified;
import org.openmole.commons.exception.ExecutionException;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.workflow.model.execution.IEnvironment;
import org.openmole.core.workflow.model.execution.IProgress;
import org.openmole.core.workflow.model.job.IMoleJob;
import org.openmole.core.workflow.model.job.State;
import org.openmole.core.workflow.model.job.IContext;
import org.openmole.core.workflow.model.task.IGenericTask;
import org.openmole.core.workflow.model.job.IMoleJobId;
import org.openmole.core.workflow.model.job.ITicket;
import org.openmole.commons.aspect.caching.SoftCachable;
import org.openmole.core.workflow.implementation.tools.FileMigrator;
import org.openmole.core.workflow.implementation.task.GenericTask;
import org.openmole.core.workflow.implementation.tools.LocalHostName;
import org.openmole.core.workflow.model.job.ITimeStamp;
import org.openmole.core.workflow.model.mole.IExecutionContext;
import org.openmole.core.workflow.model.resource.IResource;

public class MoleJob implements IMoleJob, Comparable<MoleJob> {

    final private ITicket ticket;
    final private IProgress progress;
    final private IGenericTask task;
    final private ContextContainer context;
    final private IMoleJobId id;
    volatile private State state;

    @ObjectConstructed
    public MoleJob(IGenericTask task, IContext context, ITicket ticket, IMoleJobId id) throws InternalProcessingError, UserBadDataError {
        super();
        this.context = new ContextContainer(context);
        this.task = task;
        this.ticket = ticket;
        this.id = id;
        this.progress = new Progress();

        //Context might be reused (on remote computers)
        if (!context.containsVariableWithName(GenericTask.Timestamps.getPrototype().getName())) {
            context.putVariable(GenericTask.Timestamps.getPrototype(), new ArrayList<ITimeStamp>());
        }

        setState(State.READY);
    }

    @Override
    public IContext getContext() {
        return context.getInnerContext();
    }

    @Override
    public IGenericTask getTask() {
        return task;
    }

    @Override
    public IMoleJobId getId() {
        return id;
    }

    @Override
    public State getState() {
        return state;
    }

    @ObjectModified(type = stateChanged)
    public void setState(State state) throws InternalProcessingError, UserBadDataError {
        Collection<ITimeStamp> timeStamps = getContext().getLocalValue(GenericTask.Timestamps.getPrototype());
        timeStamps.add(new TimeStamp(state, LocalHostName.getInstance().getNameForLocalHost(), System.currentTimeMillis()));
        this.state = state;
    }

    @Override
    public ITicket getTicket() {
        return ticket;
    }

    private void setInnerContext(IContext context) {
        context.setRoot(this.getContext().getRoot());
        this.context.setInnerContext(context);
    }

    @Override
    public void perform(IExecutionContext executionContext) throws InterruptedException {
        //Init variable for output filtering
        try {
            setState(State.RUNNING);
            deployRessources(executionContext);
            getTask().perform(getContext(), executionContext, progress);
            setState(State.ACHIEVED);
        } catch (Throwable e) {
            getContext().putVariable(GenericTask.Exception.getPrototype(), e);

            if (InterruptedException.class.isAssignableFrom(e.getClass())) {
                throw (InterruptedException) e;
            }
        }
    }

    @Override
    public void rethrowException(IContext context) throws ExecutionException {
        if (context.containsVariableWithName(GenericTask.Exception.getPrototype())) {
            throw new ExecutionException("Error durring job execution for task " + getTask().getName(), context.getLocalValue(GenericTask.Exception.getPrototype()));
        }
    }

    @Override
    public void finished(IContext context) throws UserBadDataError, InternalProcessingError {
        setInnerContext(context);

        if (!context.containsVariableWithName(GenericTask.Exception.getPrototype())) {
            setState(State.COMPLETED);
        } else {
            setState(State.FAILED);
        }
    }

    @Override
    public boolean isFinished() {
        return getState().isFinal();
    }

    //@Override
    public void deployRessources(IExecutionContext executionContext) throws InternalProcessingError, UserBadDataError {
        getTask().deployResources(executionContext.getLocalFileCache());
    }

    @Override
    public Iterable<IResource> getConsumedRessources() throws InternalProcessingError, UserBadDataError {
        return getTask().getResources();
    }

    @SoftCachable
    @Override
    public Iterable<File> getInputFiles() throws InternalProcessingError {
        return FileMigrator.extractFilesFromVariables(getContext());
    }

    @Override
    public int compareTo(MoleJob job) {
        return getId().compareTo(job.getId());
    }

    @Override
    public void cancel() throws InternalProcessingError, UserBadDataError {
        setState(State.CANCELED);
    }

    @Override
    public IProgress getProgress() {
        return progress;
    }


}
