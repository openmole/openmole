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
package org.openmole.core.objectidservice.internal;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.UUID;
import java.util.logging.Logger;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;
import org.openmole.core.objectidservice.IObjectIDService;

/**
 *
 * @author reuillon
 */
public class ObjectIDService implements IObjectIDService {

    final ReferenceQueue queue = new ReferenceQueue();
    final BidiMap<IdentityWeakReference, String> objectIDs = new DualHashBidiMap<IdentityWeakReference, String>();
    final Thread collector;

    public ObjectIDService() {
        collector = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    while (true) {
                        Reference ref = queue.remove();
                        if (!(ref instanceof IdentityWeakReference)) {
                            Logger.getLogger(ObjectIDService.class.getName()).severe("BUG: reference found was not of the expected class " + IdentityWeakReference.class.getName() + " but " + ref.getClass());
                        }
                        removeRef((IdentityWeakReference) ref);
                    }
                } catch (InterruptedException e) {
                    Logger.getLogger(ObjectIDService.class.getName()).fine("Collector was interrupted.");
                }
            }
        });
        collector.setDaemon(true);
        collector.start();
    }

    void interruptCollector() {
        collector.interrupt();
    }

    @Override
    public synchronized String getObjectID(Object object) {
        if (object == null) {
            throw new NullPointerException();
        }

        IdentityWeakReference ref = new IdentityWeakReference(object);
        String id = objectIDs.get(ref);

        if (id == null) {
            id = UUID.randomUUID().toString();
            objectIDs.put(ref, id);
        }

        return id;
    }

    @Override
    public synchronized void registerID(Object object, String id) {
        if (object == null) {
            throw new NullPointerException();
        }
        IdentityWeakReference ref = new IdentityWeakReference(object);
        objectIDs.put(ref, id);
    }

    private synchronized void removeRef(IdentityWeakReference ref) {
        objectIDs.remove(ref);
    }

    @Override
    public synchronized Object getObjectByID(String id) {
        IdentityWeakReference key = objectIDs.getKey(id);
        return key != null ? key.get() : null;
    }

    final class IdentityWeakReference extends WeakReference<Object> {

        final int hashCode;

        public IdentityWeakReference(Object referent) {
            super(referent, queue);
            hashCode = System.identityHashCode(referent);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }

            if (!(obj instanceof IdentityWeakReference)) {
                return false;
            }

            IdentityWeakReference ref = (IdentityWeakReference) obj;

            Object got = get();
            return got == null ? false : got == ref.get();
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}
