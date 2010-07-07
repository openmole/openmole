package org.openmole.core.implementation.observer;

/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
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



import org.openmole.core.implementation.internal.Activator;
import org.openmole.core.model.job.IMoleJob;
import org.openmole.core.model.mole.IMoleExecution;
import org.openmole.commons.aspect.eventdispatcher.IObjectChangedSynchronousListener;
import org.openmole.commons.aspect.eventdispatcher.IObjectChangedSynchronousListenerWithArgs;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.tools.structure.Priority;
import org.openmole.core.model.observer.IMoleExecutionProfiler;
import scala.Tuple2;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public abstract class MoleExecutionProfiler implements IMoleExecutionProfiler {

    class MoleExecutionProfilerOneJobFinishedAdapter implements IObjectChangedSynchronousListenerWithArgs<IMoleExecution>  {

        @Override
        public void objectChanged(IMoleExecution obj, Object[] args) throws InternalProcessingError, UserBadDataError {
            moleJobFinished((IMoleJob) args[0]);
        }

    }

    class MoleExecutionProfilerExecutionStartingAdapter implements IObjectChangedSynchronousListener<IMoleExecution>  {

        @Override
        public void objectChanged(IMoleExecution obj) throws InternalProcessingError, UserBadDataError {
            moleExecutionStarting();
        }

    }


    class MoleExecutionProfilerExecutionFinishedAdapter implements IObjectChangedSynchronousListener<IMoleExecution>  {

        @Override
        public void objectChanged(IMoleExecution obj) throws InternalProcessingError, UserBadDataError {
            moleExecutionFinished();
        }
     
    }

    final IMoleExecution moleExecution;

    public MoleExecutionProfiler(IMoleExecution moleExecution) {
        this.moleExecution = moleExecution;
        Activator.getEventDispatcher().registerListener(moleExecution, Priority.HIGH.getValue(), new MoleExecutionProfilerExecutionStartingAdapter(), IMoleExecution.starting);
        Activator.getEventDispatcher().registerListener(moleExecution, Priority.HIGH.getValue(), new MoleExecutionProfilerOneJobFinishedAdapter(), IMoleExecution.oneJobFinished);
        Activator.getEventDispatcher().registerListener(moleExecution, Priority.LOW.getValue(), new MoleExecutionProfilerExecutionFinishedAdapter(), IMoleExecution.finished);
    }

    protected abstract void moleJobFinished(IMoleJob moleJob) throws InternalProcessingError, UserBadDataError;
    protected abstract void moleExecutionStarting() throws InternalProcessingError, UserBadDataError;
    protected abstract void moleExecutionFinished() throws InternalProcessingError, UserBadDataError;

}
