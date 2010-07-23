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

import java.util.Set;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.implementation.tools.LevelComputing;
import org.openmole.core.model.job.IContext;
import org.openmole.core.model.transition.ICondition;
import org.openmole.core.model.transition.ISingleTransition;
import org.openmole.core.model.transition.ITransitionSlot;
import org.openmole.core.model.capsule.IGenericTaskCapsule;
import org.openmole.core.model.tools.ILevelComputing;
import org.openmole.core.model.capsule.ITaskCapsule;
import org.openmole.core.model.job.ITicket;
import org.openmole.core.model.mole.IMoleExecution;
import org.openmole.core.model.mole.ISubMoleExecution;

public class SingleTransition extends Transition<ITaskCapsule> implements ISingleTransition {

    public SingleTransition(ITaskCapsule start, ITransitionSlot slot, ICondition condition) {
        super(start, slot, condition);
    }

    public SingleTransition(ITaskCapsule start, ITransitionSlot slot, String condition) {
        super(start, slot, condition);
    }

    public SingleTransition(ITaskCapsule start, IGenericTaskCapsule end, String condition) {
        super(start, end, condition);
    }

    public SingleTransition(ITaskCapsule start, IGenericTaskCapsule end, ICondition condition) {
        super(start, end, condition);
    }

    public SingleTransition(ITaskCapsule start, IGenericTaskCapsule end) {
        super(start, end);
    }


    @Override
    public void performImpl(IContext global, IContext context, ITicket ticket, Set<String> toClone, IMoleExecution moleExecution, ISubMoleExecution subMole) throws InternalProcessingError, UserBadDataError {
        ILevelComputing level = LevelComputing.getLevelComputing(moleExecution);

       int beginLevel = level.getLevel(getStart());
       int endLevel = level.getLevel(getEnd().getCapsule());

       if(endLevel > beginLevel) {
           throw new UserBadDataError("The transition going from " + getStart().getAssignedTask().getName() + " to " + getEnd().getCapsule().getTask().getName() + " doesn't match the secifications.");
       }

       ITicket destTicket = ticket;
       ISubMoleExecution newSubMole = subMole;

       for(int i = beginLevel ; i > endLevel; i++) {
           destTicket = ticket.getParent();
           newSubMole = newSubMole.getParent();
       }

        synchronized (getEnd()) {
            submitNextJobsIfReady(global, context, destTicket, toClone, moleExecution, newSubMole);
        }
    }

  
    @Override
    protected void plugStart() {
        getStart().plugOutputTransition(this);
    }
}
