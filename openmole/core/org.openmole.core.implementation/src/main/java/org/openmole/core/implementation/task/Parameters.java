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
package org.openmole.core.implementation.task;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.openmole.core.implementation.data.Parameter;
import org.openmole.core.implementation.resource.FileSetResource;
import org.openmole.core.model.data.IParameter;
import org.openmole.core.model.resource.IPortable;
import org.openmole.core.model.resource.IResource;
import org.openmole.commons.exception.InternalProcessingError;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class Parameters implements Iterable<IParameter>, IPortable {

    private Map<String, IParameter> parameters = new TreeMap<String, IParameter>();
    private FileSetResource fileSet = new FileSetResource();

    @Override
    public Iterator<IParameter> iterator() {

        return new Iterator<IParameter>() {

            Iterator<IParameter> itParam = parameters.values().iterator();

            @Override
            public boolean hasNext() {
                return itParam.hasNext();
            }

            @Override
            public IParameter next() {
                IParameter ret = itParam.next();
                if (File.class.isAssignableFrom(ret.getVariable().getPrototype().getType())) {
                    ret = new Parameter(ret.getVariable().getPrototype(), fileSet.getDeployed((File) ret.getVariable().getValue()), ret.getOverride());
                }
                return ret;
            }

            @Override
            public void remove() {
                itParam.remove();
            }
        };
    }

    public IParameter put(String k, IParameter v) {
        if (File.class.isAssignableFrom(v.getVariable().getPrototype().getType())) {
            fileSet.addFile((File) v.getVariable().getValue());
        }
        return parameters.put(k, v);
    }

    public IParameter remove(String o) {
        return parameters.remove(o);
    }

    @Override
    public Iterable<IResource> getResources() throws InternalProcessingError {
        List<IResource> ret = new ArrayList<IResource>(1);
        ret.add(fileSet);
        return ret;
    }
}


