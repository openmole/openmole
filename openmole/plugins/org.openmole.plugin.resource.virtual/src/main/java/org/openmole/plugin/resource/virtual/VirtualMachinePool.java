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
package org.openmole.plugin.resource.virtual;

import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.openmole.commons.aspect.caching.Cachable;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class VirtualMachinePool extends AbstractVirtualMachinePool {

    static AtomicLong ID = new AtomicLong(0L);

    private class PooledVM implements Comparable<PooledVM> {

        final IVirtualMachine virtualMachine;
        final Long killTime;
        final Long id = ID.getAndIncrement();

        public PooledVM(IVirtualMachine virtualMachine, Long killTime) {
            this.virtualMachine = virtualMachine;
            this.killTime = killTime;
        }

        @Override
        public int compareTo(PooledVM t) {
            int compare = killTime.compareTo(t.killTime);
            if (compare != 0) {
                return compare;
            }
            return id.compareTo(t.id);

        }
    }

    private final TreeMap<Long, Future> pooledMachineKillers = new TreeMap<Long, Future>();
    private final TreeSet<PooledVM> pool = new TreeSet<PooledVM>();

    public VirtualMachinePool(VirtualMachineResource resource) {
        super(resource);
    }

    @Cachable
    private ScheduledExecutorService killer() {
        return Executors.newScheduledThreadPool(1);
    }

    @Override
    public IVirtualMachine borrowAVirtualMachine() throws UserBadDataError, InternalProcessingError {

        synchronized (pool) {
            if (!pool.isEmpty()) {
                PooledVM pooledVM = pool.pollFirst();
                pooledMachineKillers.get(pooledVM.id).cancel(false);
                return pooledVM.virtualMachine;
            }
        }
        return resource().launchAVirtualMachine();
    }

    @Override
    public void returnVirtualMachine(final IVirtualMachine virtualMachine) throws InternalProcessingError {

        long delay = delay();
        long killTime = System.currentTimeMillis() + delay;

        final PooledVM pooledVM = new PooledVM(virtualMachine, killTime);
        synchronized (pool) {
            Future future = killer().schedule(new Runnable() {

                @Override
                public void run() {
                    synchronized (pool) {
                        if (pool.contains(pooledVM)) {
                            pooledVM.virtualMachine.shutdown();
                            pool.remove(pooledVM);
                            pooledMachineKillers.remove(pooledVM.id);
                        }
                    }
                }
            }, delay, TimeUnit.MILLISECONDS);
            pooledMachineKillers.put(pooledVM.id, future);
        }
    }
}
