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

package org.openmole.core.implementation.mole;

import java.util.Arrays;
import org.openmole.core.model.execution.IMoleJobCategory;

/**
 *
 * @author reuillon
 */
public class MoleJobCategory implements IMoleJobCategory {

    private Object[] values;

    public MoleJobCategory(Object[] values) {
        this.values = values;
    }


    @Override
    public boolean equals(Object obj) {
        if(obj == null) return false;
        if(!obj.getClass().isAssignableFrom(MoleJobCategory.class)) return false;
        
        MoleJobCategory other = (MoleJobCategory) obj;

        return Arrays.deepEquals(values, other.getValues());
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(values);
    }

    public Object[] getValues() {
        return values;
    }

}
