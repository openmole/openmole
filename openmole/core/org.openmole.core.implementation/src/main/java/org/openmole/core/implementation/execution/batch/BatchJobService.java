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


package org.openmole.core.implementation.execution.batch;


import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.batchservicecontrol.UsageControl;
import org.openmole.core.batchservicecontrol.FailureControl;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.model.execution.batch.IBatchEnvironment;
import org.openmole.core.model.execution.batch.IBatchJobService;
import org.openmole.core.model.execution.batch.IBatchServiceAuthentication;
import org.openmole.core.model.execution.batch.IBatchServiceAuthenticationKey;
import org.openmole.core.model.execution.batch.IBatchServiceDescription;

public abstract class BatchJobService<ENV extends IBatchEnvironment, AUTH extends IBatchServiceAuthentication> extends BatchService<ENV, AUTH> implements IBatchJobService<ENV, AUTH> {

    public BatchJobService(ENV batchEnvironment, IBatchServiceAuthenticationKey<? extends AUTH> key, AUTH authentication, IBatchServiceDescription description, int nbAccess) throws InternalProcessingError, UserBadDataError, InterruptedException {
        super(batchEnvironment, key, authentication, description, new UsageControl(nbAccess), new FailureControl());
    }    
}
