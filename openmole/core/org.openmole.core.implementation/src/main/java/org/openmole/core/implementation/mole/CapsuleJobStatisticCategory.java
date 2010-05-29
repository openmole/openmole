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

package org.openmole.core.implementation.mole;

import org.openmole.core.workflow.model.capsule.IGenericTaskCapsule;
import org.openmole.core.workflow.model.execution.IJobStatisticCategory;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class CapsuleJobStatisticCategory implements IJobStatisticCategory {

    final IGenericTaskCapsule capsule;

    public CapsuleJobStatisticCategory(IGenericTaskCapsule capsule) {
        this.capsule = capsule;
    }

    @Override
    public int hashCode() {
        return capsule.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) return false;

        if(obj.getClass() != CapsuleJobStatisticCategory.class) return false;

        CapsuleJobStatisticCategory second = (CapsuleJobStatisticCategory) obj;

        return capsule.equals(second.capsule);
    }

}
