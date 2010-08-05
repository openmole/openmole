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
package org.openmole.core.implementation.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.implementation.tools.VariableExpansion;
import org.openmole.core.model.job.IContext;

public class EnumerationDomain<T> extends FiniteDomain<T> {

    protected List<String> enumeration;

    public EnumerationDomain() {
        enumeration = new ArrayList<String>();
    }

    public EnumerationDomain(List<String> val) {
        if (val == null) {
            enumeration = new ArrayList<String>();
        } else {
            enumeration = val;
        }
    }

    public EnumerationDomain(String... vals) {
        enumeration = new ArrayList<String>(Arrays.asList(vals));
    }

    /**
     *
     * @return the list of raw values (i.e. before any interpretation)
     */
    public Iterable<String> getRawValues() {
        return new Iterable<String>() {

            @Override
            public Iterator<String> iterator() {
                return enumeration.iterator();
            }
        };
    }
    
    public boolean add(String e) {
        return enumeration.add(e);
    }

    public int size() {
        return enumeration.size();
    }

    @Override
    public List<T> computeValues(IContext global, IContext context) throws InternalProcessingError, UserBadDataError {
        List<T> val = new ArrayList<T>(enumeration.size());
        for(String s: enumeration) {
            val.add((T) VariableExpansion.getInstance().expandData(global, context, s));
        }
        return val;
    }


}
