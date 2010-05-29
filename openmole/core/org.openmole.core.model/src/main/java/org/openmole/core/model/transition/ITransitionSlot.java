/*
 *  Copyright (C) 2010 Romain Reuillon
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


package org.openmole.core.model.transition;

import org.openmole.core.model.capsule.IGenericTaskCapsule;

/**
 *
 * The a slot for plugin in multiple transitions.
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public interface ITransitionSlot {

    /**
     *
     * Get all the transitions plugged into this slot.
     *
     * @return all the transitions plugged into this slot
     */
    Iterable<ITransition> getTransitions();

    /**
     *
     * Plug <code>transition</code> into this slot.
     *
     * @param transition the transition to plug
     */
    void plugTransition(ITransition transition);

    /**
     *
     * Unplug <code>transition</code> from thi slot.
     *
     * @param transition the transition to unplug
     */
    void unplugTransition(ITransition transition);

    /**
     *
     * Test if <code>transition</code> is plugged to this slot.
     *
     * @param transition the transition to test
     * @return true if the transition is plugged to this slot
     */
    boolean isPlugged(ITransition transition);

    /**
     *
     * Get the capsule this slot belongs to.
     *
     * 
     * @return the capsule this slot belongs to
     */
    IGenericTaskCapsule getCapsule();
}
