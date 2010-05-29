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

package org.openmole.core.replicacatalog.internal;

import org.openmole.commons.tools.io.IHash;
import org.openmole.core.workflow.model.execution.batch.IBatchEnvironmentDescription;
import org.openmole.core.workflow.model.execution.batch.IBatchServiceDescription;

/**
 *
 * @author reuillon
 */
public class ReplicaCatalogKey {
    final IHash hash;
    final IBatchServiceDescription storage;
    final IBatchEnvironmentDescription environment;

    public ReplicaCatalogKey(IHash hash, IBatchServiceDescription storage, IBatchEnvironmentDescription environment) {
        this.hash = hash;
        this.storage = storage;
        this.environment = environment;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ReplicaCatalogKey other = (ReplicaCatalogKey) obj;
        if (this.hash != other.hash && (this.hash == null || !this.hash.equals(other.hash))) {
            return false;
        }
        if (this.storage != other.storage && (this.storage == null || !this.storage.equals(other.storage))) {
            return false;
        }
        if (this.environment != other.environment && (this.environment == null || !this.environment.equals(other.environment))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + (this.hash != null ? this.hash.hashCode() : 0);
        hash = 53 * hash + (this.storage != null ? this.storage.hashCode() : 0);
        hash = 53 * hash + (this.environment != null ? this.environment.hashCode() : 0);
        return hash;
    }

    
}
