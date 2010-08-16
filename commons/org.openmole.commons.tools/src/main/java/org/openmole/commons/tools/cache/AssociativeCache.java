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
package org.openmole.commons.tools.cache;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import org.apache.commons.collections15.map.ReferenceMap;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.tools.service.LockRepository;

public class AssociativeCache<K, T> {

    public static final int WEAK = ReferenceMap.WEAK;
    public static final int SOFT = ReferenceMap.SOFT;
    public static final int HARD = ReferenceMap.HARD;

    final Map<Object, Map<K, T>> hashCache = new WeakHashMap<Object, Map<K, T>>();
    final LockRepository<K> lockRepository = new LockRepository<K>();

    final int keyRefType;
    final int valRefType;

    public AssociativeCache(int keyRefType, int valRefType) {
        this.keyRefType = keyRefType;
        this.valRefType = valRefType;
    }

    public void invalidateCache(final Object cacheAssociation, final K key) {
        final Map<K, T> cache;

        synchronized (hashCache) {
            cache = hashCache.get(cacheAssociation);
            if (cache == null) {
                return;
            }
        }

        lockRepository.lock(key);
        try {
            cache.remove(key);
        } finally {
            lockRepository.unlock(key);
        }
        
    }

    public T getCached(final Object cacheAssociation, K key) {
        final Map<K, T> cache;

        synchronized (hashCache) {
            cache = hashCache.get(cacheAssociation);
            if (cache == null) {
                return null;
            }
        }

        return cache.get(key);
    }

    public T getCache(final Object cacheAssociation, final K key, ICachable<? extends T> cachable) throws InternalProcessingError, InterruptedException, UserBadDataError {

        final Map<K, T> cache = getHashCache(cacheAssociation);

        T ret = cache.get(key);

        lockRepository.lock(key);
        try {
            if (ret == null) {

                ret = cachable.compute();
                cache.put(key, ret);

                /* lockRepository.lock(key);
                try {
                ret = cachable.compute();
                cache.put(key, ret);
                } finally {
                lockRepository.unlock(key);
                }*/
            }
        } finally {
            lockRepository.unlock(key);
        }

        /* synchronized(cache) {
        if(!cache.containsKey(key)) {
        cache.put(key, ret);
        }
        }*/

        return ret;
    }

    private Map<K, T> getHashCache(Object cacheAssociation) {
        Map<K, T> ret;
        synchronized (hashCache) {
            ret = hashCache.get(cacheAssociation);
            if (ret == null) {
                ret = Collections.synchronizedMap(new ReferenceMap(keyRefType, valRefType));
                hashCache.put(cacheAssociation, ret);
            }
        }

        return ret;
    }
}
