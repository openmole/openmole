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

package org.openmole.misc.updater.internal;

import org.openmole.misc.updater.IUpdatable;
import org.openmole.misc.updater.IUpdatableWithVariableDelay;

/**
 *
 * @author reuillon
 */
public class UpdatableWithFixedDelay implements IUpdatableWithVariableDelay {
    
    final IUpdatable updatable;
    final long delay;

    public UpdatableWithFixedDelay(IUpdatable updatable, long delay) {
        this.updatable = updatable;
        this.delay = delay;
    }

    @Override
    public long getDelay() {
        return delay;
    }

    @Override
    public boolean update() throws InterruptedException {
        return updatable.update();
    }
    
    
}
