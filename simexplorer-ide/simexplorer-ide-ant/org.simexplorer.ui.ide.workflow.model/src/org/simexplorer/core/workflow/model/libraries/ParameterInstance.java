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

public class ParameterInstance {

    private ParameterDeclaration parameterDeclaration;
    private Object value;

    public ParameterInstance(ParameterDeclaration parameterDeclaration) {
        this.parameterDeclaration = parameterDeclaration;
    }

    public Class getType() {
        return parameterDeclaration.getType();
    }

    public String getName() {
        return parameterDeclaration.getName();
    }

    public int getMetadataSize() {
        return parameterDeclaration.getMetadataSize();
    }

    public String[] getMetadataKeys() {
        return parameterDeclaration.getMetadataKeys();
    }

    public String getMetadata(String data) {
        return parameterDeclaration.getMetadata(data);
    }

    public Object getValue() {
        return value;
    }

    public Object setValue(Object value) {
        Object bak = this.value;
        this.value = value;
        return bak;
    }
}
