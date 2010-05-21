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

import org.openmole.core.workflow.implementation.data.Prototype;

public class ParameterDeclaration {

    private Prototype declaration;
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

   /* public void setType(Class type) {
        declaration.setType(type);
    }

    public void setName(String name) {
        declaration.setName(name);
    }*/

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

    public ParameterStatus getParamertStatus() {
        return paramertStatus;
    }

    public void setParamertStatus(ParameterStatus paramertStatus) {
        this.paramertStatus = paramertStatus;
    }

    @Override
    public String toString() {
         StringBuilder st=new StringBuilder();
       st.append("ParameterDeclaration : name="+getName()+",type="+getType());
        if (defaultValue!=null) st.append(",default="+defaultValue);
       // if  (getMetadata().size()>0) st.append("MetaData="+getMetadata());
        return st.toString();
    }

}
