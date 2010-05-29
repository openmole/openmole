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

package org.openmole.core.implementation.transition;

import org.openmole.commons.aspect.eventdispatcher.IObjectChangedSynchronousListenerWithArgs;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.workflow.model.job.IMoleJob;
import org.openmole.core.workflow.model.mole.IExecutionContext;
import org.openmole.core.workflow.model.mole.IMoleExecution;
import org.openmole.core.workflow.model.mole.ISubMoleExecution;

/**
 *
 * @author reuillon
 */
public class AggregationTransitionAdapter implements IObjectChangedSynchronousListenerWithArgs<ISubMoleExecution>{

    final AggregationTransition transition;

    public AggregationTransitionAdapter(AggregationTransition transition) {
        this.transition = transition;
    }

    @Override
    public void objectChanged(ISubMoleExecution subMole, Object[] args) throws InternalProcessingError, UserBadDataError {
        if(!IMoleJob.class.isAssignableFrom(args[0].getClass()) || !IExecutionContext.class.isAssignableFrom(args[1].getClass())) {
            throw new InternalProcessingError("BUG: argument of the event has the wrong type.");
        }

        IMoleJob lastJob = (IMoleJob) args[0];
        IExecutionContext execution = (IExecutionContext) args[1];
        transition.subMoleFinished(subMole, lastJob, execution);
    }

}
