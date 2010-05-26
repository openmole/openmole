/*
 *
 *  Copyright (c) 2008, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston,
 *  MA  02110-1301  USA
 */
package org.openmole.core.workflow.implementation.task;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.aspect.caching.ChangeState;
import org.openmole.core.workflow.model.execution.IProgress;
import org.openmole.core.workflow.model.resource.ILocalFileCache;
import org.openmole.core.workflow.model.resource.IResource;
import org.openmole.core.workflow.model.job.IContext;
import org.openmole.core.workflow.model.task.IGenericTask;
import org.openmole.core.workflow.model.task.annotations.Resource;
import org.openmole.core.workflow.model.task.annotations.Input;
import org.openmole.core.workflow.model.task.annotations.Output;
import org.openmole.commons.aspect.caching.SoftCachable;

import org.openmole.core.workflow.implementation.data.Data;
import org.openmole.core.workflow.implementation.data.Parameter;
import org.openmole.core.workflow.model.data.IVariable;
import org.openmole.core.workflow.model.data.IData;
import org.openmole.core.workflow.model.data.IParameter;
import org.openmole.core.workflow.model.data.IPrototype;
import org.openmole.core.workflow.model.mole.IExecutionContext;

import static org.openmole.core.workflow.implementation.tools.MarkedFieldFinder.*;

public abstract class GenericTask implements IGenericTask {

    @Output
    final static public IData<Collection> Timestamps = new Data<Collection>("Timestamps", Collection.class);
    @Output
    final static public IData<Throwable> Exception = new Data<Throwable>("Exception", Throwable.class, true);

    private String name;

    private Map<String, IData<?>> input;
    private Map<String, IData<?>> output;

    private Set<IResource> resources;

    private Parameters parameters = new Parameters();


    public GenericTask(String name) {
        setName(name);
    }

    /**
     * Verifies that variable specified as input are well presents in the context
     * @param context
     * @throws org.openmole.core.UserBadDataError
     * @throws org.openmole.core.InternalProcessingError
     */
    protected void verifyInput(IContext context) throws UserBadDataError, InternalProcessingError {
        for (IData<?> d : getInput()) {
            if (!d.isOptional()) {
                IPrototype<?> p = d.getPrototype();

                IVariable<?> v = context.getVariable(p.getName());
                if (v == null) {
                    throw new UserBadDataError(null, "Input data named \"" + p.getName() + "\" of type \"" + p.getType().getName() + "\" required by the task \"" + getName() + "\" has not been found");
                } else if (!p.isAssignableFrom(v.getPrototype())) {
                    throw new UserBadDataError(null, "Input data named \"" + p.getName() + "\" required by the task \"" + getName() + "\" has the wrong type: \"" + v.getPrototype().getType().getName() + "\" instead of \"" + p.getType().getName() + "\" required");
                }
            }
        }
    }

    protected void filterOutput(IContext context) throws UserBadDataError, InternalProcessingError {
        List<IVariable<?>> vars = new LinkedList<IVariable<?>>();
        for (IData<?> d : getOutput()) {
            IPrototype<?> p = d.getPrototype();
            IVariable<?> var = context.getLocalVariable(p);
            if (var != null) {
                if (p.getType().isAssignableFrom(var.getValue().getClass())) {
                    vars.add(var);
                } else {
                    Logger.getLogger(GenericTask.class.getName()).log(Level.WARNING, "Variable " + p.getName() + " of type " + p.getType().getName() + " has been found but type doesn't match : " + var.getPrototype().getType().getName() + " in task " + getName() + ".");
                }
            } else {
                if (!d.isOptional()) {
                    Logger.getLogger(GenericTask.class.getName()).log(Level.WARNING, "Variable " + p.getName() + " of type " + p.getType().getName() + " not found in output of task " + getName() + ".");
                }
            }
        }

        context.clean();
        context.setVariables(vars);
    }

    private void init(IContext context) throws UserBadDataError, InternalProcessingError {
        for (IParameter parameter : getParameters()) {
            if (parameter.getOverride() || !context.containsVariableWithName(parameter.getVariable().getPrototype())) {
                context.putVariable(parameter.getVariable().getPrototype(), parameter.getVariable().getValue());
            }
        }

        verifyInput(context);
    }

    /**
     * The main operation of the processor.
     * @param context
     * @param progress
     * @throws org.openmole.core.UserBadDataError
     * @throws org.openmole.core.InternalProcessingError
     * @throws InterruptedException
     */
    protected abstract void process(IContext context, IExecutionContext executionContext, IProgress progress) throws UserBadDataError, InternalProcessingError, InterruptedException;

    /* (non-Javadoc)
     * @see org.openmole.core.processors.ITask#run(org.openmole.core.processors.ApplicativeContext)
     */
    @Override
    public void perform(IContext context, IExecutionContext executionContext, IProgress progress) throws InternalProcessingError, UserBadDataError, InterruptedException {
        try {
            init(context);
            process(context, executionContext, progress);
            end(context);
        } catch (InternalProcessingError e) {
            throw new InternalProcessingError(e, "Error in task " + getName());
        } catch (UserBadDataError e) {
            throw new UserBadDataError(e, "Error in task " + getName());
        }
    }

    private void end(IContext context) throws UserBadDataError, InternalProcessingError {
        filterOutput(context);
    }

    /* (non-Javadoc)
     * @see org.openmole.core.processors.ITask#addOutPrototype(org.openmole.core.data.structure.Prototype)
     */
    @Override
    public void addOutput(IPrototype prototype) {
        addOutput(prototype, false);
    }

    @Override
    public void addOutput(IPrototype prototype, boolean optional) {
        addOutput(new Data(prototype, optional));
    }

    @ChangeState
    @Override
    public synchronized void addOutput(IData data) {
        if (output == null) {
            output = new TreeMap<String, IData<?>>();
        }
        output.put(data.getPrototype().getName(), data);
    }

    @ChangeState
    @Override
    public synchronized void addResource(IResource resource) {
        if (resources == null) {
            resources = new HashSet<IResource>();
        }
        resources.add(resource);
    }

    @ChangeState
    @Override
    public synchronized void removeResource(IResource resource) {
        if (resources != null) {
            resources.remove(resource);
        }
    }

    


    /* (non-Javadoc)
     * @see org.openmole.core.processors.ITask#addInPrototype(org.openmole.core.data.structure.Prototype)
     */
    @Override
    public void addInput(IPrototype prototype) {
        addInput(prototype, false);
    }

    @Override
    public void addInput(IPrototype prototype, boolean optional) {
        addInput(new Data(prototype, optional));
    }

    @ChangeState
    @Override
    public synchronized void addInput(IData data) {
        if (input == null) {
            input =  new TreeMap<String, IData<?>>();
        }
        input.put(data.getPrototype().getName(),data);
    }

    @Override
    public  void removeInput(IData data) {
       removeInput(data.getPrototype());
    }

    @Override
    public synchronized void removeInput(IPrototype prototype) {
       removeInput(prototype.getName());
    }

    @Override
    public synchronized void removeInput(String prototype) {
        if(input != null) {
            input.remove(prototype);
        }
    }

    @Override
    public void removeOutput(IData data) {
        removeOutput(data.getPrototype());
    }

    @Override
    public void removeOutput(IPrototype prototype) {
        removeOutput(prototype.getName());
    }


    @Override
    public synchronized void removeOutput(String prototype) {
        if(output != null) {
            output.remove(prototype);
        }
    }


    /* (non-Javadoc)
     * @see org.openmole.core.processors.ITask#getName()
     */
    @Override
    public String getName() {
        return name;
    }

    /* (non-Javadoc)
     * @see org.openmole.core.processors.ITask#setName(java.lang.String)
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    /* (non-Javadoc)
     * @see org.openmole.core.processors.ITask#getInput()
     */
    @SoftCachable
    @Override
    public List<IData<?>> getInput() throws InternalProcessingError {
        List<IData<?>> tmpInputCache;
        tmpInputCache = new LinkedList<IData<?>>();
        if (input != null) {
            tmpInputCache.addAll(input.values());
        }

        addAllMarkedFields(this, Input.class, tmpInputCache);
        verifyNotDuplicate(tmpInputCache, "input");

        return tmpInputCache;
    }

    /* (non-Javadoc)
     * @see org.openmole.core.processors.ITask#getOutput()
     */
    @SoftCachable
    @Override
    public List<IData<?>> getOutput() throws InternalProcessingError {
        List<IData<?>> tmpOutputCache;
        tmpOutputCache = new LinkedList<IData<?>>();
        if (output != null) {
            tmpOutputCache.addAll(output.values());
        }
        addAllMarkedFields(this, Output.class, tmpOutputCache);

        verifyNotDuplicate(tmpOutputCache, "output");

        return tmpOutputCache;
    }


    public Iterable<IData<?>> getUserInput() {
        return input.values();
    }

    public Iterable<IData<?>> getUserOutput() {
        return output.values();
    }

    private void verifyNotDuplicate(Iterable<IData<?>> data, String place) {
        Set<String> processed = new HashSet<String>();

        for (IData<?> d : data) {
            if (processed.contains(d.getPrototype().getName())) {
                Logger.getLogger(GenericTask.class.getName()).log(Level.WARNING, "Variable with name " + d.getPrototype().getName() + " is mentioned twice in " + place + " of task " + getName() + ".");
            } else {
                processed.add(d.getPrototype().getName());
            }
        }
    }

    @SoftCachable
    @Override
    public Collection<IResource> getResources() throws InternalProcessingError, UserBadDataError {
        Collection<IResource> resourcesCache = new LinkedList<IResource>();
        if (resources != null) {
            resourcesCache.addAll(resources);
        }
        
        addAllMarkedFields(this, Resource.class, resourcesCache);
        
        for(IResource resource: getParameters().getResources()) {
            resourcesCache.add(resource);
        }

        return resourcesCache;
    }


    @Override
    public void deployResources(ILocalFileCache fileCache) throws InternalProcessingError, UserBadDataError {
        for (IResource resource : getResources()) {
            resource.deploy(fileCache);
        }
    }

    @ChangeState
    @Override
    public void addParameter(IParameter<?> parameter) {
        parameters.put(parameter.getVariable().getPrototype().getName(),parameter);
    }

    @Override
    public void removeParameter(IPrototype<?> prototype) {
        removeParameter(prototype.getName());
    }

    @ChangeState
    @Override
    public void removeParameter(String name) {
        parameters.remove(name);
    }

    @Override
    public <T> void addParameter(IPrototype<? super T> prototype, T value) {
        addParameter(new Parameter<T>(prototype, value));
    }

    @Override
    public <T> void addParameter(IPrototype<? super T> prototype, T value, boolean override) {
        addParameter(new Parameter<T>(prototype, value, override));
    }

    @Override
    public Parameters getParameters() {
        return parameters;
    }


    @Override
    public String toString() {
        return getName();
    }


}
