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
package org.openmole.core.implementation.task;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.openmole.core.model.data.IParameter;
import org.openmole.core.model.data.IVariable;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class Parameters implements Iterable<IParameter> {

    private Map<String, IParameter> parameters = new TreeMap<String, IParameter>();

    @Override
    public Iterator<IParameter> iterator() {
        return parameters.values().iterator();
    }

    public void put(IParameter parameter) {
        parameters.put(parameter.getVariable().getPrototype().getName(), parameter);
    }

    public void remove(String name) {
        parameters.remove(name);
    }

    Iterable<IVariable> getVariables(){
        List<IVariable> variables = new ArrayList<IVariable>(parameters.size());

        for(IParameter parameter: parameters.values()) {
            variables.add(parameter.getVariable());
        }

        return variables;
    }

}


