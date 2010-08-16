/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
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

package org.openmole.core.implementation.resource;

import java.util.Collection;
import java.util.LinkedList;
import org.openmole.core.model.resource.IResource;
import org.openmole.core.model.task.annotations.Resource;
import org.openmole.commons.aspect.caching.SoftCachable;

import static org.openmole.core.implementation.tools.MarkedFieldFinder.*;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public abstract class ComposedResource implements IResource {

    @Override
    public void deploy() throws InternalProcessingError, UserBadDataError {
        for(IResource resourse: getComposedResources()) {
            resourse.deploy();
        }
    }

    @SoftCachable
    private Iterable<IResource> getComposedResources() throws InternalProcessingError {
        Collection<IResource> ret = new LinkedList<IResource>();
        addAllMarkedFields(this, Resource.class, ret);
        return ret;
    }

}
