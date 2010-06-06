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

package org.openmole.core.implementation.execution.batch;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.model.execution.batch.IBatchEnvironmentAuthentication;
import org.openmole.core.model.execution.batch.IBatchJobService;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public abstract class BatchEnvironmentAuthentication implements IBatchEnvironmentAuthentication {

    boolean isAccessInitialized = false;

    @Override
    public synchronized void initializeAccessIfNeeded() throws UserBadDataError, InternalProcessingError, InterruptedException {
        if(!isAccessInitialized) {
            initializeAccess();
            isAccessInitialized = true;
        }
    }

    @Override
    public boolean isAccessInitialized() {
        return isAccessInitialized;
    }


}
