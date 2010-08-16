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

package org.openmole.core.authenticationregistry.internal;

import java.util.HashMap;
import java.util.Map;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.authenticationregistry.IAuthenticationRegistry;
import org.openmole.core.model.execution.batch.IBatchServiceAuthentication;
import org.openmole.core.model.execution.batch.IBatchServiceAuthenticationKey;

/**
 *
 * @author reuillon
 */
public class AuthenticationRegistry implements IAuthenticationRegistry {

    final Map<IBatchServiceAuthenticationKey, IBatchServiceAuthentication> registry = new HashMap<IBatchServiceAuthenticationKey, IBatchServiceAuthentication>();
    
    @Override
    public synchronized  boolean isRegistred(IBatchServiceAuthenticationKey authenticationKey) {
        return registry.containsKey(authenticationKey);
    }

    @Override
    public synchronized <AUTH extends IBatchServiceAuthentication> void initAndRegisterIfNotAllreadyIs(IBatchServiceAuthenticationKey<? extends AUTH> key, AUTH authentication) throws InternalProcessingError, UserBadDataError, InterruptedException {
        if(!isRegistred(key)) {
            authentication.initialize();
            registry.put(key, authentication);
        }
    }

    @Override
    public <AUTH extends IBatchServiceAuthentication> AUTH getRegistred(IBatchServiceAuthenticationKey<? extends AUTH> key) {
        return (AUTH) registry.get(key);
    }

}
