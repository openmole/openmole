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

package org.openmole.plugin.environment.glite;

import java.net.URI;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.plugin.environment.jsaga.JSAGAJob;
import org.openmole.plugin.environment.jsaga.JSAGAJobService;

/**
 *
 * @author reuillon
 */
public class GliteJobService extends JSAGAJobService<GliteEnvironment, GliteAuthentication> {

    public GliteJobService(URI jobServiceURI, GliteEnvironment environment, GliteAuthenticationKey authenticationKey, GliteAuthentication authentication, int nbAccess) throws InternalProcessingError, UserBadDataError, InterruptedException {
        super(jobServiceURI, environment, authenticationKey, authentication, nbAccess);
    }

    @Override
    protected JSAGAJob buildJob(String id) throws InternalProcessingError {
        return new GliteJob(id, this, getAuthentication().getProxyExpiresTime());
    }
    
}
