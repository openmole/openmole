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
package org.openmole.core.implementation.observer;

import java.io.File;
import java.util.Set;
import java.util.TreeSet;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.implementation.internal.Activator;
import org.openmole.core.implementation.job.Context;
import org.openmole.core.model.capsule.IGenericTaskCapsule;
import org.openmole.core.model.data.IData;
import org.openmole.core.model.data.IVariable;
import org.openmole.core.model.job.IContext;
import org.openmole.core.model.job.IMoleJob;
import org.openmole.core.model.mole.IMoleExecution;
import org.openmole.core.model.observer.ISaver;
import org.openmole.core.serializer.ISerializationResult;

/**
 *
 * @author reuillon
 */
public class Saver implements ISaver, IMoleExecutionObserver {

    final File dir;
    
    public Saver(IMoleExecution moleExection, IGenericTaskCapsule taskCapsule, File dir) {
        new MoleExecutionObserverAdapter(moleExection, this);
        this.dir = dir;
    }
    
    public Saver(IMoleExecution moleExection, IGenericTaskCapsule taskCapsule, String dir) {
        this(moleExection, taskCapsule, new File(dir));
    }
    
    @Override
    public synchronized void moleJobFinished(IMoleJob moleJob) throws InternalProcessingError, UserBadDataError {
        Set<String> filter = new TreeSet<String>();
        
        for(IData data: moleJob.getTask().getOutput()) {
            if(data.getMode().isSystem()) filter.add(data.getPrototype().getName());
        }
        
        IContext context = new Context();
        
        for(IVariable variable: moleJob.getContext()) {
            if(!filter.contains(variable.getPrototype().getName())) context.putVariable(variable);
        }
        
        ISerializationResult serializationResult = Activator.getSerializer().serializeAndGetPluginClassAndFiles(context, dir);
        for(File file: serializationResult.getFiles()) {
            
        }
    }

    @Override
    public void moleExecutionStarting() throws InternalProcessingError, UserBadDataError {
        if(!dir.exists()) dir.mkdirs();
    }

    @Override
    public void moleExecutionFinished() throws InternalProcessingError, UserBadDataError {
        
    }


}
