/*
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
package org.simexplorer.core.workflow.model.libraries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.implementation.data.Prototype;
import org.simexplorer.core.workflow.model.metada.Metadata;
import org.simexplorer.core.workflow.model.metada.MetadataModifier;
import org.simexplorer.core.workflow.model.metada.MetadataProvider;


public class MethodDeclaration implements MetadataProvider, MetadataModifier, Iterable<ParameterDeclaration> {

    private Metadata metadata;
    private String name;
    private List<ParameterDeclaration> parameters;
    private transient HashMap<String,ParameterDeclaration> parametersCache;

    public MethodDeclaration(String name) throws InternalProcessingError {
        this.name = name;
        this.metadata = new Metadata();
        this.parameters = new ArrayList<ParameterDeclaration>();
        this.parametersCache = new HashMap<String,ParameterDeclaration>();
    }

    @Override
    public String getMetadata(String data) {
        return metadata.get(data);
    }

    @Override
    public int getMetadataSize() {
        return metadata.size();
    }

    @Override
    public String[] getMetadataKeys() {
        return metadata.keys();
    }

    @Override
    public void setMetadata(String key, String value) {
        metadata.set(key, value);
    }

    @Override
    public Metadata getMetadata() {
        return metadata;
    }

    @Override
    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private HashMap<String,ParameterDeclaration> getParametersCache() {
        if (parametersCache == null) {
                parametersCache=new HashMap<String,ParameterDeclaration>();
         }
        return parametersCache;
    }

    @Override
    public Iterator<ParameterDeclaration> iterator() {
        return parameters.iterator();
    }

    public Iterable<ParameterDeclaration> getParameters() {
        return parameters;
    }

    public ParameterDeclaration getParameter(String name) throws InternalProcessingError {
        return getParametersCache().get(name);
    }

    public int getParametersSize() {
        return parameters.size();
    }

    public boolean add(ParameterDeclaration e) {
        if (parametersCache != null) {
            parametersCache.put(e.getName(),e);
            return true;
        } else {
            // if null, we don't try do put directly in cache to avoid exception risk (and throws declaration)
            return parameters.add(e);
        }
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
        return getParametersCache().get(name).setDefaultValue(defaultValue);
    }
}

