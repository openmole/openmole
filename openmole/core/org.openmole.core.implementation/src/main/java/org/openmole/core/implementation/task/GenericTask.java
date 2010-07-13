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
package org.openmole.core.implementation.task;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.aspect.caching.ChangeState;
import org.openmole.core.model.data.IDataSet;
import org.openmole.core.model.execution.IProgress;
import org.openmole.core.model.resource.IResource;
import org.openmole.core.model.job.IContext;
import org.openmole.core.model.task.IGenericTask;
import org.openmole.core.model.task.annotations.Resource;
import org.openmole.core.model.task.annotations.Input;
import org.openmole.core.model.task.annotations.Output;
import org.openmole.commons.aspect.caching.SoftCachable;

import org.openmole.core.implementation.data.Data;
import org.openmole.core.implementation.data.DataSet;
import org.openmole.core.implementation.data.Parameter;
import org.openmole.core.model.data.DataModeMask;
import org.openmole.core.model.data.IVariable;
import org.openmole.core.model.data.IData;
import org.openmole.core.model.data.IParameter;
import org.openmole.core.model.data.IPrototype;

import static org.openmole.core.model.data.DataModeMask.*;
import static org.openmole.core.implementation.tools.MarkedFieldFinder.*;

public abstract class GenericTask implements IGenericTask {

    @Output
    final static public IData<Collection> Timestamps = new Data<Collection>("Timestamps", Collection.class, SYSTEM);

    @Output
    final static public IData<Throwable> Exception = new Data<Throwable>("Exception", Throwable.class, OPTIONAL, SYSTEM);

    final private String name;

    final private Map<String, IData<?>> input = new TreeMap<String, IData<?>>();;
    final private Map<String, IData<?>> output = new TreeMap<String, IData<?>>();;

    final private Set<IResource> resources = new HashSet<IResource>();

    final private Parameters parameters = new Parameters();

    public GenericTask(String name) {
        this.name = name;
    }

    /**
     * Verifies that variable specified as input are well presents in the context
     * @param context
     * @throws org.openmole.core.UserBadDataError
     * @throws org.openmole.core.InternalProcessingError
     */
    protected void verifyInput(IContext context) throws UserBadDataError, InternalProcessingError {
        for (IData<?> d : getInput()) {
            if (!d.getMode().isOptional()) {
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
                if (!d.getMode().isOptional()) {
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
    protected abstract void process(IContext context, IProgress progress) throws UserBadDataError, InternalProcessingError, InterruptedException;

    /* (non-Javadoc)
     * @see org.openmole.core.processors.ITask#run(org.openmole.core.processors.ApplicativeContext)
     */
    @Override
    public void perform(IContext context, IProgress progress) throws InternalProcessingError, UserBadDataError, InterruptedException {
        try {
            deploy();
            init(context);
            process(context, progress);
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
        addOutput(new Data(prototype));
    }

    @Override
    public void addOutput(IPrototype prototype, DataModeMask... masks) {
        addOutput(new Data(prototype, masks));
    }

    @ChangeState
    @Override
    public synchronized void addOutput(IData data) {
        output.put(data.getPrototype().getName(), data);
    }

    @ChangeState
    @Override
    public synchronized void addResource(IResource resource) {
        resources.add(resource);
    }

    /* (non-Javadoc)
     * @see org.openmole.core.processors.ITask#addInPrototype(org.openmole.core.data.structure.Prototype)
     */
    @Override
    public void addInput(IPrototype prototype) {
        addInput(new Data(prototype));
    }

    @Override
    public void addInput(IPrototype prototype, DataModeMask... masks) {
        addInput(new Data(prototype, masks));
    }

    @ChangeState
    @Override
    public synchronized void addInput(IData data) {
        input.put(data.getPrototype().getName(),data);
    }

    @Override
    public void addInput(IDataSet dataSet) {
        for(IData data: dataSet) {
            addInput(data);
        }
    }

    @Override
    public void addOutput(IDataSet dataSet) {
        for(IData data: dataSet) {
            addOutput(data);
        }
    }

    @Override
    public synchronized boolean containsInput(String name) {
        if(input == null) return false;
        return input.containsKey(name);
    }

    @Override
    public boolean containsInput(IPrototype prototype) {
        return containsInput(prototype.getName());
    }

    @Override
    public synchronized  boolean containsOutput(String name) {
        if(output == null) return false;
        return output.containsKey(name);
    }

    @Override
    public boolean containsOutput(IPrototype prototype) {
        return output.containsKey(prototype.getName());
    }

    /* (non-Javadoc)
     * @see org.openmole.core.processors.ITask#getName()
     */
    @Override
    public String getName() {
        return name;
    }

    /* (non-Javadoc)
     * @see org.openmole.core.processors.ITask#getInput()
     */
    @SoftCachable
    @Override
    public IDataSet getInput() throws InternalProcessingError, UserBadDataError {
        List<IData<?>> tmpInputCache;
        tmpInputCache = new LinkedList<IData<?>>();
        if (input != null) {
            tmpInputCache.addAll(input.values());
        }

        addAllMarkedFields(this, Input.class, tmpInputCache);
        //verifyNotDuplicate(tmpInputCache, "input");

        return new DataSet(tmpInputCache);
    }

    /* (non-Javadoc)
     * @see org.openmole.core.processors.ITask#getOutput()
     */
    @SoftCachable
    @Override
    public IDataSet getOutput() throws InternalProcessingError, UserBadDataError {
        List<IData<?>> tmpOutputCache;
        tmpOutputCache = new LinkedList<IData<?>>();
        if (output != null) {
            tmpOutputCache.addAll(output.values());
        }
        addAllMarkedFields(this, Output.class, tmpOutputCache);

        return new DataSet(tmpOutputCache);
    }


    public Iterable<IData<?>> getUserInput() {
        return input.values();
    }

    public Iterable<IData<?>> getUserOutput() {
        return output.values();
    }

    @SoftCachable
    public Iterable<IResource> getResources() throws InternalProcessingError, UserBadDataError {
        Collection<IResource> resourcesCache = new HashSet<IResource>();
        if (resources != null) {
            resourcesCache.addAll(resources);
        }
        
        addAllMarkedFields(this, Resource.class, resourcesCache);
        
        return resourcesCache;
    }


    @Override
    public void deploy() throws InternalProcessingError, UserBadDataError {
        for (IResource resource : getResources()) {
            resource.deploy();
        }
    }
    
    @ChangeState
    @Override
    public void addParameter(IParameter<?> parameter) {
        parameters.put(parameter);
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
