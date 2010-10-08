/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
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
package org.openmole.core.implementation.task;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.model.data.IDataSet;
import org.openmole.core.model.execution.IProgress;
import org.openmole.core.model.job.IContext;
import org.openmole.commons.exception.MultipleException;
import org.openmole.commons.aspect.caching.SoftCachable;
import org.openmole.commons.aspect.eventdispatcher.IObjectChangedSynchronousListenerWithArgs;
import org.openmole.commons.tools.service.Priority;
import org.openmole.core.implementation.data.DataSet;
import org.openmole.core.implementation.internal.Activator;
import org.openmole.core.implementation.job.Context;
import org.openmole.core.implementation.job.SynchronizedContext;
import org.openmole.core.implementation.mole.MoleExecution;
import org.openmole.core.model.data.IData;
import org.openmole.core.model.data.IPrototype;
import org.openmole.core.model.job.IMoleJob;
import org.openmole.core.model.job.State;
import org.openmole.core.model.mole.IMole;
import org.openmole.core.model.mole.IMoleExecution;
import org.openmole.core.model.task.IMoleTask;

public class MoleTask extends Task implements IMoleTask {

    class ExceptionLister implements IObjectChangedSynchronousListenerWithArgs<IMoleExecution> {

        final Collection<Throwable> throwables = Collections.synchronizedCollection(new LinkedList<Throwable>());

        @Override
        public void objectChanged(IMoleExecution t, Object[] os) throws InternalProcessingError, UserBadDataError {
            IMoleJob moleJob = (IMoleJob) os[0];

            if (moleJob.getState() == State.FAILED) {
                Throwable exception = moleJob.getContext().getValue(GenericTask.Exception.getPrototype());
                throwables.add(exception);
            }
        }

        public Collection<Throwable> getThrowables() {
            return throwables;
        }
    }
    IMole mole;

    public MoleTask(String name, IMole workflow)
            throws UserBadDataError, InternalProcessingError {
        super(name);
        this.mole = workflow;
    }

    @Override
    protected void process(IContext global, IContext context, IProgress progress) throws UserBadDataError, InternalProcessingError, InterruptedException {

        IContext globalContext = new SynchronizedContext();
        IContext firstTaskContext = new Context();

        for (IData<?> input : getInput()) {
            if (!input.getMode().isOptional() || input.getMode().isOptional() && context.contains(input.getPrototype())) {
                IPrototype p = input.getPrototype();
                firstTaskContext.putVariable(p, context.getValue(p));
            }
        }

        IMoleExecution execution = new MoleExecution(mole, globalContext, firstTaskContext);

        ExceptionLister exceptionLister = new ExceptionLister();
        Activator.getEventDispatcher().registerListener(execution, Priority.NORMAL.getValue(), exceptionLister, IMoleExecution.oneJobFinished);

        execution.start();
        execution.waitUntilEnded();

        for (IData<?> output : getUserOutput()) {
            IPrototype p = output.getPrototype();
            if (globalContext.contains(p)) {
                context.putVariable(p, globalContext.getValue(p));
            }
        }

        Collection<Throwable> exceptions = exceptionLister.getThrowables();

        if (!exceptions.isEmpty()) {
            context.putVariable(GenericTask.Exception.getPrototype(), new MultipleException(exceptions));
        }
    }

    @Override
    public IMole getMole() {
        return mole;
    }

    @SoftCachable
    @Override
    public IDataSet getInput() throws InternalProcessingError, UserBadDataError {
        return new DataSet(super.getInput(), getMole().getRoot().getAssignedTask().getInput());
    }
}
