/*
 *
 *  Copyright (c) 2008, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston,
 *  MA  02110-1301  USA
 */
package org.openmole.core.implementation.plan;

import java.util.Collection;
import java.util.LinkedList;

import org.openmole.core.workflow.model.plan.IFactor;
import org.openmole.core.implementation.data.Prototype;
import org.openmole.core.workflow.model.data.IPrototype;
import org.openmole.core.workflow.model.domain.IDomain;
import org.openmole.core.workflow.model.resource.IResource;
import org.openmole.core.workflow.model.task.annotations.Resource;
import org.openmole.commons.exception.InternalProcessingError;

import static org.openmole.core.implementation.tools.MarkedFieldFinder.*;

public class Factor<T, D extends IDomain<? extends T>> implements IFactor<T, D> {

    private IPrototype<T> prototype;
    private D domain;

    private Factor() {
    }

    public Factor(String name, Class<T> type, D domain) {
        super();
        this.prototype = new Prototype<T>(name, type);
        this.domain = domain;
    }

    public Factor(IPrototype<T> prototype, D domain) {
        super();
        this.prototype = prototype;
        this.domain = domain;
    }

    /* (non-Javadoc)
     * @see org.openmole.core.plan.IFactor#getDomain()
     */
    @Override
    public D getDomain() {
        return domain;
    }

    /* (non-Javadoc)
     * @see org.openmole.core.plan.IFactor#setDomain(org.openmole.core.data.factors.domain.Domain)
     */
    public void setDomain(D domain) {
        this.domain = domain;
    }

    @Override
    public IPrototype<T> getPrototype() {
        return prototype;
    }

    public void setPrototype(IPrototype<T> prototype) {
        this.prototype = prototype;
    }

   
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Factor<T, D> other = (Factor<T, D>) obj;
        if (this.prototype != other.prototype && (this.prototype == null || !this.prototype.equals(other.prototype))) {
            return false;
        }
        if (this.domain != other.domain && (this.domain == null || !this.domain.equals(other.domain))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 17 * hash + (this.prototype != null ? this.prototype.hashCode() : 0);
        hash = 17 * hash + (this.domain != null ? this.domain.hashCode() : 0);
        return hash;
    }

    @Override
    public Collection<IResource> getResources() throws InternalProcessingError {
        Collection<IResource> resourcesCache = new LinkedList<IResource>();
        addAllMarkedFields(this, Resource.class, resourcesCache);
        return resourcesCache;
    }

    
}
