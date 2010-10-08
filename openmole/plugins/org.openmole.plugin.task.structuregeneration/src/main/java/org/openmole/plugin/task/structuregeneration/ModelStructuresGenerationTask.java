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

package org.openmole.plugin.task.structuregeneration;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.implementation.task.Task;
import org.openmole.core.implementation.data.Data;
import org.openmole.core.model.execution.IProgress;
import org.openmole.core.model.job.IContext;
import org.openmole.core.model.task.annotations.Output;

/**
 *
 * @author reuillon
 */
public class ModelStructuresGenerationTask extends Task {

    @Output
    public final static Data<Object> InputData = new Data<Object>("input", Object.class);
    
    @Output
    public final static Data<Object> OutputData = new Data<Object>("output", Object.class);

    private Class<?> inputDataStructureClass;
    private Class<?> outputDataStructureClass;

    public ModelStructuresGenerationTask(String name, Class<?> inputDataStructure, Class<?> outputDataStructure) throws UserBadDataError, InternalProcessingError {
        super(name);
        this.inputDataStructureClass = inputDataStructure;
        this.outputDataStructureClass = outputDataStructure;
    }

    /* (non-Javadoc)
     * @see org.openmole.methods.task.IExploration#getInputDataStructureClass()
     */
    public synchronized  Class<?> getInputDataStructureClass() throws InternalProcessingError {
        return inputDataStructureClass;
    }

    /* (non-Javadoc)
     * @see org.openmole.methods.task.IExploration#getOutputDataStructureClass()
     */
    public synchronized  Class<?> getOutputDataStructureClass() throws InternalProcessingError {
         return outputDataStructureClass;
    }

    @Override
    protected void process(IContext global, IContext context, IProgress progress) throws UserBadDataError, InternalProcessingError, InterruptedException {
        try {
            context.putVariable(InputData.getPrototype(), getInputDataStructureClass().newInstance());
            context.putVariable(OutputData.getPrototype(), getOutputDataStructureClass().newInstance());
        } catch (InstantiationException e) {
            throw new InternalProcessingError(e);
        } catch (IllegalAccessException e) {
            throw new InternalProcessingError(e);
        }

    }
}
