/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
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

package org.openmole.core.implementation.transition;

import java.util.HashSet;
import java.util.Set;

import org.openmole.core.model.transition.IGenericTransition;
import org.openmole.core.model.transition.ISlot;
import org.openmole.core.model.capsule.IGenericTaskCapsule;

public class Slot implements ISlot {

    Set<IGenericTransition> transitions = new HashSet<IGenericTransition>();
    IGenericTaskCapsule<?, ?> end;

    public Slot(IGenericTaskCapsule<?, ?> end) {
        setEnd(end);
    }

    @Override
    public void plugTransition(IGenericTransition transition) {
        transitions.add(transition);
    }

    @Override
    public void unplugTransition(IGenericTransition transition) {
        transitions.remove(transition);
    }

    @Override
    public Iterable<IGenericTransition> getTransitions() {
        return transitions;
    }

    @Override
    public boolean isPlugged(IGenericTransition transition) {
        return transitions.contains(transition);
    }


    @Override
    public IGenericTaskCapsule<?, ?> getCapsule() {
        return end;
    }

    public void setEnd(IGenericTaskCapsule<?, ?> end) {
        this.end = end;
    }
}
