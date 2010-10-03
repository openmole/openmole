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
package org.openmole.core.implementation.job;

import org.openmole.core.model.job.IMoleJobId;

public class MoleJobId implements IMoleJobId {

    final String executionId;
    final Long id;

    public MoleJobId(String executionId, Long id) {
        super();
        this.executionId = executionId;
        this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MoleJobId other = (MoleJobId) obj;
        if ((this.executionId == null) ? (other.executionId != null) : !this.executionId.equals(other.executionId)) {
            return false;
        }
        if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + (this.executionId != null ? this.executionId.hashCode() : 0);
        hash = 53 * hash + (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

 

    @Override
    public String toString() {
        return Long.toString(id);
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public int compareTo(IMoleJobId o) {
        int compare = getId().compareTo(o.getId());
        if(compare != 0) return compare;
        return executionId.compareTo(o.getExecutionId());
    }

    @Override
    public String getExecutionId() {
        return executionId;
    }
}
