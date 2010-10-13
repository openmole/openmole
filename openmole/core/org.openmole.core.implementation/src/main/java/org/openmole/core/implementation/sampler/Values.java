/*
 *
 *  Copyright (c) 2008, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License as
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
package org.openmole.core.implementation.sampler;

import org.openmole.core.model.sampler.IValues;

import java.util.Map;
import java.util.TreeMap;
import org.openmole.core.model.data.IPrototype;

public class Values implements IValues {

    final private Map<String, Object> valuesByName = new TreeMap<String, Object>();

    public <T> void setValue(IPrototype<? super T> prototype, T value) {
        setValue(prototype.getName(), value);
    }

    public void setValue(String name, Object value) {
        valuesByName.put(name, value);
    }
    
    public void addAll(IValues from) {
        for(String name: from.getNames()) {
            setValue(name, from.getValue(name));
        }
    }
    
    /* (non-Javadoc)
     * @see org.openmole.core.plan.IFactorValue#getValue(java.lang.String)
     */
    @Override
    public <T> T getValue(IPrototype<? super T> factorPrototype) {
        return (T) getValue(factorPrototype.getName());
    }

    @Override
    public String toString() {
        return valuesByName.toString();
    }

    @Override
    public Object getValue(String name) {
        return valuesByName.get(name);
    }

    @Override
    public Iterable<String> getNames() {
        return valuesByName.keySet();
    }
}
