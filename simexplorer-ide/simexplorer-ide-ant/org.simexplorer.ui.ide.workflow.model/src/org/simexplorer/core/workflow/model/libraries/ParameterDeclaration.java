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

import org.openmole.core.implementation.data.Prototype;
import org.simexplorer.core.workflow.model.metada.Metadata;
import org.simexplorer.core.workflow.model.metada.MetadataModifier;
import org.simexplorer.core.workflow.model.metada.MetadataProvider;

public class ParameterDeclaration implements MetadataProvider, MetadataModifier {

    private Prototype declaration;
    private Metadata metadata=new Metadata();
    private Object defaultValue;
    private ParameterStatus paramertStatus=ParameterStatus.optional;

    public enum ParameterStatus{
        optional,mandatory
    }

    public ParameterDeclaration(Prototype declaration, Object defaultValue) {
        this.declaration = declaration;
        this.defaultValue = defaultValue;
    }

    public Class getType() {
        return declaration.getType();
    }

    public String getName() {
        return declaration.getName();
    }

    public Object getDefaultValue() {
        return defaultValue;
    }


    /**
     * Set the default value and returns the previous setted default value (could be null)
     * @param defaultValue
     * @return The previous setted default value (could be null)
     */
    public Object setDefaultValue(Object defaultValue) {
        Object bak = this.defaultValue;
        this.defaultValue = defaultValue;
        return bak;
    }

      @Override
    public void setMetadata(Metadata metadata) {
        this.metadata=metadata;
    }

    @Override
    public void setMetadata(String key, String value) {
        metadata.set(key, value);
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
    public Metadata getMetadata() {
        return metadata;
    }

    @Override
    public String getMetadata(String data) {
        return metadata.get(data);
    }

    public ParameterStatus getParamertStatus() {
        return paramertStatus;
    }

    public void setParamertStatus(ParameterStatus paramertStatus) {
        this.paramertStatus = paramertStatus;
    }

    @Override
    public String toString() {
         StringBuilder st=new StringBuilder();
       st.append("ParameterDeclaration : name=").append(getName()).append(",type=").append(getType());
        if (defaultValue!=null) st.append(",default=").append(defaultValue);
//        if  (getMetadata().size()>0) st.append("MetaData=").append(getMetadata());
        return st.toString();
    }

}
