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
package org.openmole.core.implementation.data;

import java.util.Collection;
import org.openmole.core.workflow.model.data.IPrototype;

public class Prototype<T> implements IPrototype<T> {

    final private String name;
    final private Class<? extends T> type;


    //Dirty private constructor for building list prototypes
    private Prototype(String name, Class type, boolean unChecked) {
        this.name = name;
        this.type = type;
    }

    public Prototype(String name, Class<? extends T> type) {
        this.name = name;
        this.type = type;
    }

    public Prototype(IPrototype<? extends T> prototype, String name) {
        this(name, prototype.getType());
    }

    public Prototype(IPrototype<? extends T> prototype,  Class<? extends T> type) {
        this(prototype.getName(), type);
    }

    @Override
    public String getName() {
        return name;
    }

   /* public void setName(String name) {
        this.name = name;
    }*/

    @Override
    public Class<? extends T> getType() {
        return type;
    }

    /*public void setType(Class type) {
        this.type = type;
    }
*/
    @Override
    public boolean isAssignableFrom(IPrototype<?> p) {
    	return getType().isAssignableFrom(p.getType());
    }

    @Override
    public IPrototype<Collection<T>> array() {
    	return new Prototype<Collection<T>>(name, Collection.class, true);
    }

    @Override
    public String toString() {
        return '(' + type.getName() + ')' + getName();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Prototype<T> other = (Prototype<T>) obj;
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        if (this.type != other.type && (this.type == null || !this.type.equals(other.type))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 89 * hash + (this.type != null ? this.type.hashCode() : 0);
        return hash;
    }

	
}
