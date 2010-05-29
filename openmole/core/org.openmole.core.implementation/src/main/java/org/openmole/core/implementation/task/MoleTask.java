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

package org.openmole.core.implementation.task;

import java.util.Collection;
import java.util.HashSet;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.workflow.model.execution.IProgress;
import org.openmole.core.workflow.model.resource.ILocalFileCache;
import org.openmole.core.workflow.model.job.IContext;
import org.openmole.commons.exception.MultipleException;
import org.openmole.commons.aspect.caching.SoftCachable;
import org.openmole.core.implementation.job.Context;
import org.openmole.core.implementation.mole.MoleExecution;
import org.openmole.core.workflow.model.data.IData;
import org.openmole.core.workflow.model.data.IPrototype;
import org.openmole.core.workflow.model.mole.IMole;
import org.openmole.core.workflow.model.mole.IExecutionContext;
import org.openmole.core.workflow.model.mole.IMoleExecution;
import org.openmole.core.workflow.model.resource.IResource;
import org.openmole.core.workflow.model.task.IMoleTask;

public class MoleTask extends Task implements IMoleTask {

    IMole workflow;

    public MoleTask(String name, IMole workflow)
            throws UserBadDataError, InternalProcessingError {
        super(name);
        this.workflow = workflow;
    }

    @Override
    protected void process(IContext context, IExecutionContext executionContext, IProgress progress) throws UserBadDataError, InternalProcessingError, InterruptedException {

        IContext rootContext = new Context();
        IContext firstTaskContext = new Context(rootContext);

        for (IData<?> input : getInput()) {
            if (!input.isOptional() || input.isOptional() && context.contains(input.getPrototype())) {
                IPrototype p = input.getPrototype();
                firstTaskContext.putVariable(p, context.getLocalValue(p));
            }
        }
        
        IMoleExecution execution = workflow.createExecution(firstTaskContext, executionContext);
        execution.start();
        execution.waitUntilEnded();

        for (IData<?> output : getOutput()) {
            IPrototype p = output.getPrototype();
            if (rootContext.contains(p)) {
                context.putVariable(p, rootContext.getGlobalValue(p));
            }
        }

        Collection<Throwable> exceptions = rootContext.getLocalValue(MoleExecution.Exceptions);

        if (exceptions != null && !exceptions.isEmpty()) {
            context.putVariable(GenericTask.Exception.getPrototype(), new MultipleException(exceptions));
        }

    }

    @Override
    public IMole getMole() {
        return workflow;
    }

    @SoftCachable
    @Override
    public Collection<IResource> getResources() throws InternalProcessingError, UserBadDataError {
        Collection<IResource> resources = new HashSet<IResource>();

        for (IResource res : super.getResources()) {
            resources.add(res);
        }

        for (IResource res : getMole().getAllRessources()) {
            resources.add(res);
        }

        return resources;
    }

    //The resources of the task of the inner workflow will be deployer durring it's execution
    @Override
    public void deployResources(ILocalFileCache fileCache)
            throws InternalProcessingError, UserBadDataError {

        for (IResource resource : super.getResources()) {
            resource.deploy(fileCache);
        }
    }
}
