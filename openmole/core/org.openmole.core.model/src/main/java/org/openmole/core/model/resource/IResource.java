/*
 *  Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.model.resource;

import java.io.File;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

/**
 *
 * A resource is a component that can be embeded. Resources can be polymorphous
 * and deploy diferents component depending on the environment the are deployed on.
 * For instance binary executable compiled for diferent environments.
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public interface IResource {

    /**
     *
     * Deploy the resource on the local environment.
     *
     * @throws InternalProcessingError  if something goes wrong because of a system failure
     * @throws UserBadDataError         if something goes wrong because it is missconfigured
     */
    void deploy() throws InternalProcessingError, UserBadDataError;

}
