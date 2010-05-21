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
package org.openmole.core.workflow.methods.libraries;

import java.net.URI;
import java.util.ArrayList;
import org.openmole.misc.exception.InternalProcessingError;
import org.openmole.core.workflow.implementation.data.Prototype;
import org.openmole.core.workflow.methods.libraries.Platform.Type;

public class MethodInstance {

    private Library library;
    private MethodDeclaration method;
   // private CachedArrayList<ParameterInstance, String> parameters;

    public MethodInstance(Library library, MethodDeclaration method) throws InternalProcessingError {
        throw new UnsupportedOperationException("Not supported yet.");
        /*this.library = library;
        this.method = method;
        try {
            this.parameters = new CachedArrayList<ParameterInstance, String>(new ArrayList<ParameterInstance>(method.getParametersSize()), ParameterInstance.class.getDeclaredMethod("getName"));
        } catch (NoSuchMethodException ex) {
            throw new InternalProcessingError(ex);
        } catch (SecurityException ex) {
            throw new InternalProcessingError(ex);
        }
        for (ParameterDeclaration parameter : method.getParameters()) {
            this.parameters.add(new ParameterInstance(parameter));
        }*/
    }

    public String getMethodName() {
        return method.getName();
    }


    public Object setParameterValue(Prototype parameterPrototype, Object paramValue) {
        return setParameterValue(parameterPrototype.getName(), paramValue);
    }

    public Object setParameterValue(String parameterName, Object paramValue) {
        throw new UnsupportedOperationException("Not supported yet.");
        //return parameters.get(parameterName).setValue(paramValue);
    }

    public Object getParameterValue(String parameterName) {
        throw new UnsupportedOperationException("Not supported yet.");
       // return parameters.get(parameterName).getValue();
    }

    public String[] getParametersNames() {
        throw new UnsupportedOperationException("Not supported yet.");
      /*  String[] names = new String[method.getParametersSize()];
        for (int i = 0; i < names.length; i++) {
            names[i] = parameters.getList().get(i).getName();
        }
        return names;*/
    }

    public Class[] getParametersTypes() {
        throw new UnsupportedOperationException("Not supported yet.");
      /*  Class[] types = new Class[method.getParametersSize()];
        for (int i = 0; i < types.length; i++) {
            types[i] = parameters.getList().get(i).getType();
        }
        return types;*/
    }

    public Object[] getParametersValues() {
        throw new UnsupportedOperationException("Not supported yet.");
      /*  Object[] values = new Object[method.getParametersSize()];
        for (int i = 0; i < values.length; i++) {
            values[i] = parameters.getList().get(i).getValue();
        }
        return values;*/
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

}
