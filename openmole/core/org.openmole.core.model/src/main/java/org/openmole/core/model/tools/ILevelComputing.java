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
package org.openmole.core.model.tools;

import org.openmole.core.model.capsule.IGenericTaskCapsule;

/**
 *
 * Compute the level of a task capsule in a mole. The level of the root capsule is 0.
 * The level is increased by 1 when the mole execution goes through an exploration
 * transition and decreased by 1 when it goes through an aggregation transition.
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public interface ILevelComputing {

    /**
     *
     * Get the level of the given capsule. Return an infinite value if the capsule
     * doesn't belong to the mole.
     *
     * @param capsule the capsule for which computing the level
     * @return the level of the capsule or an infinite value if the capule doesn't belong to the mole
     */
    int getLevel(IGenericTaskCapsule<?, ?> capsule);

}
