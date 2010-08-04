/*
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
package org.simexplorer.core.workflow.model.libraries;

import java.net.URI;
import java.util.HashMap;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.implementation.data.Prototype;
import org.simexplorer.core.workflow.model.libraries.Platform.Type;
import org.simexplorer.core.workflow.model.metada.MetadataProvider;

public class MethodInstance implements MetadataProvider {

    private Library library;
    private MethodDeclaration method;
    private HashMap<String,ParameterInstance> parameters;

    public MethodInstance(Library library, MethodDeclaration method) throws InternalProcessingError {
        this.library = library;
        this.method = method;
            this.parameters = new HashMap<String,ParameterInstance>();
        for (ParameterDeclaration parameter : method.getParameters()) {
            this.parameters.put(parameter.getName(),new ParameterInstance(parameter));
        }
    }

    public String getMethodName() {
        return method.getName();
    }

    @Override
    public String getMetadata(String data) {
        return method.getMetadata().get(data);
    }

    @Override
    public int getMetadataSize() {
        return method.getMetadata().size();
    }

    @Override
    public String[] getMetadataKeys() {
        return method.getMetadata().keys();
    }

    public Object setParameterValue(Prototype parameterPrototype, Object paramValue) {
        return setParameterValue(parameterPrototype.getName(), paramValue);
    }

    public Object setParameterValue(String parameterName, Object paramValue) {
        return parameters.get(parameterName).setValue(paramValue);
    }

    public Object getParameterValue(String parameterName) {
        return parameters.get(parameterName).getValue();
    }

    public String[] getParametersNames() {
        return parameters.keySet().toArray(new String[]{});
    }

    public Class[] getParametersTypes() {
        Class[] types = new Class[method.getParametersSize()];
        int i=0;
        for (String p:parameters.keySet()) {
            types[i] = parameters.get(p).getType();
        }
        return types;
    }

    public Object[] getParametersValues() {
        Object[] values = new Object[method.getParametersSize()];
        int i=0;
        for (String p:parameters.keySet()) {
            values[i] = parameters.get(p).getType();
        }
        return values;
    }

    public URI getLibraryUri() {
        return library.getUri();
    }

    public Type getLibraryPlatform() {
        return library.getPlatform();
    }

    public String getLibraryName() {
        return library.getName();
    }

    public String getLibraryMetadata(String name) {
        return library.getMetadata().get(name);
    }
}
