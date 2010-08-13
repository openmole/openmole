/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Affero GNU General Public License as published by
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
package org.openmole.core.implementation.mole;

import java.util.logging.Logger;
import org.openmole.core.implementation.internal.Activator;
import org.openmole.core.model.mole.ISubMoleExecution;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

/**
 *
 * @author reuillon
 */
public class SubMoleExecution implements ISubMoleExecution {

    int nbJobInProgress = 0;
    int nbJobWaitingInGroup = 0;

    ISubMoleExecution parent;

    public SubMoleExecution() {
        this.parent = this;
    }

    public SubMoleExecution(ISubMoleExecution parent) {
        this.parent = parent;
    }

    @Override
    public int getNbJobInProgess() {
        return nbJobInProgress;
    }

    @Override
    public void incNbJobInProgress() throws InternalProcessingError, UserBadDataError {
       nbJobInProgress++;
       if(!isRoot()) getParent().incNbJobInProgress();
    }

    @Override
    public void incNbJobWaitingInGroup() throws InternalProcessingError, UserBadDataError {
        nbJobWaitingInGroup++;
        checkAllJobsWaitingInGroup();
    }

    @Override
    public void decNbJobWaitingInGroup() {
        nbJobWaitingInGroup--;
    }

    @Override
    public void decNbJobWaitingInGroup(int value) {
        nbJobWaitingInGroup -= value;
    }

    void checkAllJobsWaitingInGroup() throws InternalProcessingError, UserBadDataError  {
        if(nbJobInProgress == nbJobWaitingInGroup && nbJobWaitingInGroup > 0) {
            Activator.getEventDispatcher().objectChanged(this, allJobsWaitingInGroup);
        }
    }

    @Override
    public void decNbJobInProgress() throws InternalProcessingError, UserBadDataError {
        nbJobInProgress--;
        checkAllJobsWaitingInGroup();
        if(!isRoot()) getParent().decNbJobInProgress();
    }

    @Override
    public ISubMoleExecution getParent() {
        return parent;
    }

    @Override
    public boolean isRoot() {
        return this == getParent();
    }

    @Override
    public void incNbJobInProgress(int val) throws InternalProcessingError, UserBadDataError {
        nbJobInProgress += val;
        if(!isRoot()) getParent().incNbJobInProgress(val);
    }

    @Override
    public void decNbJobInProgress(int val) throws InternalProcessingError, UserBadDataError {
        nbJobInProgress -= val;
        checkAllJobsWaitingInGroup();
        if(!isRoot()) getParent().decNbJobInProgress(val);
    }

}
