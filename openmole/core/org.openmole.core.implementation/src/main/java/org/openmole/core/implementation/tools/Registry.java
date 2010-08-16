/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Affero GNU General Public License as published by
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
package org.openmole.core.implementation.tools;

import org.openmole.core.model.tools.IRegistry;
import java.util.Collections;
import java.util.HashMap;

import java.util.Map;

public class Registry<K, V> implements IRegistry<K, V> {

    private Map<K, V> registry = Collections.synchronizedMap(new HashMap<K, V>());

    public Registry() {
    }

    @Override
    public boolean isRegistredFor(K key) {
        return registry.containsKey(key);
    }

    @Override
    public void register(K key, V object) {
        registry.put(key, object);
    }

    @Override
    public V consult(K key) {
        return registry.get(key);
    }

    @Override
    public V removeFromRegistry(K key) {
        return registry.remove(key);
    }
}
