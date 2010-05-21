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

package org.openmole.ui.console.internal.command.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class Registry {

    final private static Registry instance = new Registry();

    public static Registry getInstance() {
        return instance;
    }

    final List<Object> registry = Collections.synchronizedList(new LinkedList<Object>());

    public void register(Object object) {
        if(object == null) throw new IllegalArgumentException("Argument should not be null.");
        registry.add(object);
    }

    public List<Object> getRegistred() {
        return registry;
    }

    public <T> List<T> getRegistred(Class<T> cl) {
        List<T> ret = new ArrayList<T>();
        synchronized(registry) {
            for(Object object: registry) {
                if(cl.isAssignableFrom(object.getClass())) {
                    ret.add((T)object);
                }
            }
        }
        return ret;
    }

     public <T> T getRegistred(Class<T> cl, int i) {
         return getRegistred(cl).get(i);
     }

      public Object getRegistred(int i) {
         return registry.get(i);
     }
}
