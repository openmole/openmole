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

package org.openmole.core.implementation.observer;

import org.openmole.commons.aspect.eventdispatcher.IObjectChangedSynchronousListener;
import org.openmole.commons.aspect.eventdispatcher.IObjectChangedSynchronousListenerWithArgs;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.tools.structure.Priority;
import org.openmole.core.implementation.internal.Activator;
import org.openmole.core.model.job.IMoleJob;
import org.openmole.core.model.mole.IMoleExecution;

/**
 *
 * @author reuillon
 */
public class MoleExecutionObserverAdapter {
    
    
    class MoleExecutionOneJobFinishedAdapter implements IObjectChangedSynchronousListenerWithArgs<IMoleExecution>  {

        @Override
        public void objectChanged(IMoleExecution obj, Object[] args) throws InternalProcessingError, UserBadDataError {
            moleExecutionObserver.moleJobFinished((IMoleJob) args[0]);
        }

    }

    class MoleExecutionExecutionStartingAdapter implements IObjectChangedSynchronousListener<IMoleExecution>  {

        @Override
        public void objectChanged(IMoleExecution obj) throws InternalProcessingError, UserBadDataError {
            moleExecutionObserver.moleExecutionStarting();
        }

    }


    class MoleExecutionExecutionFinishedAdapter implements IObjectChangedSynchronousListener<IMoleExecution>  {

        @Override
        public void objectChanged(IMoleExecution obj) throws InternalProcessingError, UserBadDataError {
            moleExecutionObserver.moleExecutionFinished();
        }
    }
    
    final IMoleExecutionObserver moleExecutionObserver;

    public MoleExecutionObserverAdapter(IMoleExecution moleExecution, IMoleExecutionObserver moleExecutionObserver) {
        this.moleExecutionObserver = moleExecutionObserver;
        Activator.getEventDispatcher().registerListener(moleExecution, Priority.HIGH.getValue(), new MoleExecutionExecutionStartingAdapter(), IMoleExecution.starting);
        Activator.getEventDispatcher().registerListener(moleExecution, Priority.HIGH.getValue(), new MoleExecutionOneJobFinishedAdapter(), IMoleExecution.oneJobFinished);
        Activator.getEventDispatcher().registerListener(moleExecution, Priority.LOW.getValue(), new MoleExecutionExecutionFinishedAdapter(), IMoleExecution.finished);
    }
    
}
