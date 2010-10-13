/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
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
package org.openmole.core.implementation.execution.batch;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.implementation.internal.Activator;
import org.openmole.core.model.execution.batch.IBatchService;
import org.openmole.core.model.execution.batch.IBatchServiceDescription;
import org.openmole.core.batchservicecontrol.IFailureControl;
import org.openmole.core.batchservicecontrol.IUsageControl;
import org.openmole.core.model.execution.batch.IBatchEnvironment;
import org.openmole.core.model.execution.batch.IBatchServiceAuthentication;
import org.openmole.core.model.execution.batch.IBatchServiceAuthenticationKey;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public abstract class BatchService<ENV extends IBatchEnvironment, AUTH extends IBatchServiceAuthentication> implements IBatchService<ENV, AUTH> {

    final private IBatchServiceDescription description;
    final private ENV batchEnvironment;
    final private IBatchServiceAuthenticationKey<? extends AUTH> key;

    public BatchService(ENV batchEnvironment, IBatchServiceAuthenticationKey<? extends AUTH> key, AUTH authentication, IBatchServiceDescription description, IUsageControl usageControl, IFailureControl failureControl) throws InternalProcessingError, UserBadDataError, InterruptedException {
        Activator.getBatchEnvironmentAuthenticationRegistry().initAndRegisterIfNotAllreadyIs(key, authentication);
        Activator.getBatchRessourceControl().registerRessouce(description, usageControl, failureControl);
        
        this.description = description;
        this.batchEnvironment = batchEnvironment;
        this.key = key;
    }

    @Override
    public IBatchServiceDescription getDescription() {
        return description;
    }

    @Override
    public ENV getEnvironment() {
        return batchEnvironment;
    }

    @Override
    public String toString() {
        return getDescription().toString();
    }

    @Override
    public AUTH getAuthentication() {
        return Activator.getBatchEnvironmentAuthenticationRegistry().getRegistred(getAuthenticationKey());
    }

    @Override
    public IBatchServiceAuthenticationKey<? extends AUTH> getAuthenticationKey() {
        return key;
    }
    
}
