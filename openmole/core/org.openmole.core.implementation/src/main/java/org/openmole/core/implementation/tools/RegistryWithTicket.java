/*
 *  Copyright (C) 2010 reuillon
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
package org.openmole.core.implementation.tools;

import org.openmole.core.model.tools.IRegistry;
import org.openmole.core.model.tools.IRegistryWithTicket;
import java.util.Map;
import java.util.WeakHashMap;

import org.openmole.core.model.job.ITicket;

public class RegistryWithTicket<K, V> implements IRegistryWithTicket<K, V> {

    Map<ITicket, IRegistry<K, V>> registries = new WeakHashMap<ITicket, IRegistry<K, V>>();

    synchronized IRegistry<K, V> getRegistry(ITicket ticket) {
        IRegistry<K, V> ret = registries.get(ticket);
        if (ret == null) {
            ret = new Registry<K, V>();
            registries.put(ticket, ret);
        }
        return ret;
    }

    @Override
    public V consult(K key, ITicket ticket) {
        return getRegistry(ticket).consult(key);
    }

    @Override
    public boolean isRegistredFor(K key, ITicket ticket) {
        return getRegistry(ticket).isRegistredFor(key);
    }

    @Override
    public void register(K key, ITicket ticket, V object) {
        getRegistry(ticket).register(key, object);
    }

    @Override
    public V removeFromRegistry(K key, ITicket ticket) {
        return getRegistry(ticket).removeFromRegistry(key);
    }
}
