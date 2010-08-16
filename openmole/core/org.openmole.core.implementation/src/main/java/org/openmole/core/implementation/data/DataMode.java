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

package org.openmole.core.implementation.data;

import org.openmole.core.model.data.DataModeMask;
import org.openmole.core.model.data.IDataMode;
import static org.openmole.core.model.data.DataModeMask.*;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class DataMode implements IDataMode {

    final static public DataMode NONE = new DataMode(0);

    int mask;

    public DataMode(DataModeMask... masks) {
        mask = 0;
        for(DataModeMask m: masks) {
            mask |= m.getMask();
        }
    }


    public DataMode(int mask) {
        this.mask = mask;
    }

    @Override
    public boolean isOptional() {
        return (mask & OPTIONAL.getMask()) != 0;
    }

    @Override
    public boolean isImmutable() {
        return (mask & IMMUTABLE.getMask()) != 0;
    }

    @Override
    public boolean isSystem() {
       return (mask & SYSTEM.getMask()) != 0;
    }

}
