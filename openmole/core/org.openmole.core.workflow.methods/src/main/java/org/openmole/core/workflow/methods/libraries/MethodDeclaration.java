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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openmole.misc.exception.InternalProcessingError;
import org.openmole.core.workflow.implementation.data.Prototype;
//import org.openmole.misc.tools.CachedArrayList;

public class MethodDeclaration implements Iterable<ParameterDeclaration> {

    private String name;
    private List<ParameterDeclaration> parameters;
   // private transient CachedArrayList<ParameterDeclaration, String> parametersCache;

    public MethodDeclaration(String name) throws InternalProcessingError {
        throw new UnsupportedOperationException("Not supported yet.");
       /* this.name = name;
        this.parameters = new ArrayList<ParameterDeclaration>();
        try {
            this.parametersCache = new CachedArrayList<ParameterDeclaration, String>(parameters, ParameterDeclaration.class.getDeclaredMethod("getName"));
        } catch (NoSuchMethodException ex) {
            throw new InternalProcessingError(ex);
        } catch (SecurityException ex) {
            throw new InternalProcessingError(ex);
        }*/
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

  /*  private CachedArrayList<ParameterDeclaration, String> getParametersCache() throws InternalProcessingError {


        if (parametersCache == null) {
            try {
                new CachedArrayList<ParameterDeclaration, String>(parameters, ParameterDeclaration.class.getDeclaredMethod("getName"));
            } catch (NoSuchMethodException ex) {
                throw new InternalProcessingError(ex);
            } catch (SecurityException ex) {
                throw new InternalProcessingError(ex);
            }
        }
        return parametersCache;
    }*/

    @Override
    public Iterator<ParameterDeclaration> iterator() {
        return parameters.iterator();
    }

    public Iterable<ParameterDeclaration> getParameters() {
        return parameters;
    }

    public ParameterDeclaration getParameter(String name) throws InternalProcessingError {
        throw new UnsupportedOperationException("Not supported yet.");
        //return getParametersCache().get(name);
    }

    public int getParametersSize() {
        return parameters.size();
    }

    public boolean add(ParameterDeclaration e) {
        throw new UnsupportedOperationException("Not supported yet.");
        /*if (parametersCache != null) {
            return parametersCache.add(e);
        } else {
            // if null, we don't try do put directly in cache to avoid exception risk (and throws declaration)
            return parameters.add(e);
        }*/
    }

    public void addParameter(Prototype parameter) {
        add(new ParameterDeclaration(parameter, null));
    }

    public void addParameter(Prototype parameter, Object defaultValue) {
        add(new ParameterDeclaration(parameter, defaultValue));
    }

    public void addParameter(String name, Class type) {
        this.addParameter(new Prototype(name, type));
    }

    public void addParameter(String name, Class type, Object defaultValue) {
        this.addParameter(new Prototype(name, type), defaultValue);
    }

    /**
     * Set the default value for a parameter.
     * @param name
     * @param defaultValue
     * @return The precedent default value. null if it this parameter didn't have a default value.
     */
    public Object setParameterDefaultValue(String name, Object defaultValue) throws InternalProcessingError {
        throw new UnsupportedOperationException("Not supported yet.");
        //return getParametersCache().get(name).setDefaultValue(defaultValue);
    }
}

