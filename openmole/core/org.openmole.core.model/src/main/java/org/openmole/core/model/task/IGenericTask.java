/*
 *  Copyright (C) 2010 Romain Reuillon
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
package org.openmole.core.model.task;

import java.util.Collection;
import org.openmole.core.model.data.IData;
import org.openmole.core.model.data.IParameter;
import org.openmole.core.model.data.IPrototype;
import org.openmole.core.model.job.IContext;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.model.data.DataModMask;
import org.openmole.core.model.execution.IProgress;
import org.openmole.core.model.mole.IExecutionContext;
import org.openmole.core.model.resource.ILocalFileCache;
import org.openmole.core.model.resource.IPortable;
import org.openmole.core.model.resource.IResource;

/**
 *
 * Generic interface for all tasks in OpenMOLE.
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public interface IGenericTask extends IPortable {

    /**
     *
     * Perform this task.
     *
     * @param context the context in which it is executed
     * @param executionContext the execution context in which it is executed
     * @param progress the progress reporting structure of the task
     * @throws InternalProcessingError something went wrong for a reason that can't be directly related to a user defined parameter
     * @throws UserBadDataError something went wrong for a reason that can be directly related to a user defined parameter
     * @throws InterruptedException the thread has been interupted
     */
    void perform(IContext context, IExecutionContext executionContext, IProgress progress) throws InternalProcessingError, UserBadDataError, InterruptedException;

    /**
     *
     * Get the name of the task.
     *
     * @return
     */
    String getName();

    /**
     *
     * Set the name of the task.
     *
     * @param name the new name for the task
     */
    void setName(String name);

    /**
     *
     * Get the input data of the task.
     *
     * @return the input of the task
     * @throws InternalProcessingError in case something went wrong looking for the input by reflexion for instance
     */
    Collection<IData<?>> getInput() throws InternalProcessingError;

    /**
     *
     * Get the output data of the task.
     *
     * @return the output data of the task
     * @throws InternalProcessingError in case something went wrong looking for the output by reflexion for instance
     */
    Collection<IData<?>> getOutput() throws InternalProcessingError;

    /**
     *
     * Add <code>resource</code> to this task.
     *
     * @param ressource the resource to be added
     */
    void addResource(IResource ressource);

    /**
     *
     * Remove <code>resource</code> from the resources of this tasks.
     *
     * @param ressource the resource to be removed
     */
    void removeResource(IResource ressource);

    /**
     *
     * Deploy the resource locally.
     *
     * @param environment the environment for polymorphous resources having different beaviour depending on the environment
     * @param localFileCache the local file cache containing the local copies of the files
     * @throws InternalProcessingError something went wrong for a reason that can't be directly related to a user defined parameter
     * @throws UserBadDataError something went wrong for a reason that can be directly related to a user defined parameter
     */
    void deployResources(ILocalFileCache localFileCache) throws InternalProcessingError, UserBadDataError;

    /**
     *
     * Add <code>data</code> as an input for this task.
     *
     * @param data the data added in input
     */
    void addInput(IData data);


    void addInput(IPrototype prototype, DataModMask... masks);

    /**
     *
     * Add a non optional data constructed from <code>prototype</code> as an input for this task.
     *
     * @param prototype the prototype of the data
     */
    void addInput(IPrototype prototype);

    /**
     *
     * Remove the user defined input data.
     *
     * @param data the data to remove of the input
     */
    void removeInput(IData data);

    /**
     *
     * Remove the user defined input data given its prototype.
     *
     * @param prototype the prototype of the data to remove
     */
    void removeInput(IPrototype prototype);

    /**
     *
     * Remove the user defined input data given its name.
     *
     * @param name the name of the data to remove
     */
    void removeInput(String name);

    /**
     *
     * Add <code>data</code> as an output for this task.
     *
     * @param data the data to add
     */
    void addOutput(IData data);


    void addOutput(IPrototype prototype, DataModMask... masks);

    /**
     *
     * Add a non optional data constructed from <code>prototype</code> as an output for this task.
     *
     * @param prototype prototype the prototype of the data
     */
    void addOutput(IPrototype prototype);

    /**
     *
     * Remove the user defined output data.
     *
     * @param data the data to remove
     */
    void removeOutput(IData data);

    /**
     *
     * Remove the user defined output data given its prototype.
     *
     * @param prototype the prototype of the data to remove
     */
    void removeOutput(IPrototype prototype);

    /**
     *
     * Remove the user defined input data given its name.
     *
     * @param name the name of the data to remove
     */
    void removeOutput(String name);

    /**
     *
     * Add a parameter for this task.
     *
     * @param parameter     the parameter to add
     */
    void addParameter(IParameter<?> parameter);

    /**
     *
     * Add a parameter for this task.
     *
     * @param <T> a super type type of the parameter
     * @param prototype     the prototype of the parameter
     * @param value         the value of the parameter
     */
    <T> void addParameter(IPrototype<? super T> prototype, T value);

    /**
     *
     * Add a parameter for this task.
     *
     * @param <T> a super type type of the parameter
     * @param prototype prototype the prototype of the parameter
     * @param value         value the value of the parameter
     * @param override      true if the parameter should override an existing value
     */
    <T> void addParameter(IPrototype<? super T> prototype, T value, boolean override);

    /**
     *
     * Remove a parameter allready configured in this task.
     *
     * @param prototype     the prototype of the parameter to remove
     */
    void removeParameter(IPrototype<?> prototype);

    /**
     *
     * Remove a parameter allready configured in this task.
     *
     * @param name          the name of the parameter to remove
     */
    void removeParameter(String name);

    /**
     *
     * Get all the parameters configured for this task.
     *
     * @return the parameters configured for this task.
     */
    Iterable<IParameter> getParameters();
}
