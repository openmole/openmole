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

import org.openmole.core.workflow.methods.libraries.Platform.Type;

import java.net.URI;
import java.util.ArrayList;

public class Library  {

    private String name;
    private Platform.Type platform;
    private URI uri;
    private ArrayList<MethodDeclaration> methods;
    private ArrayList<Library> dependancies;

    public Library(String name, Type platform) {
        this.name = name;
        this.platform = platform;
        methods = new ArrayList<MethodDeclaration>();
    }

    public void addMethod(MethodDeclaration m) {
        methods.add(m);
    }

    public ArrayList<MethodDeclaration> getMethods() {
        return methods;
    }
    

    public ArrayList<Library> getDependancies() {
        return dependancies;
    }

    public void setDependancies(ArrayList<Library> dependancies) {
        this.dependancies = dependancies;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getPlatform() {
        return platform;
    }

    public void setPlatform(Type platform) {
        this.platform = platform;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }
}

