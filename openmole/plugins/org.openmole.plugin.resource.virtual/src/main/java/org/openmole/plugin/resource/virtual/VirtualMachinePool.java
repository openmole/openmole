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

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.collections15.bidimap.TreeBidiMap;
import org.openmole.misc.exception.InternalProcessingError;
import org.openmole.misc.exception.UserBadDataError;
import org.openmole.misc.workspace.ConfigurationLocation;
import org.openmole.plugin.resource.virtual.internal.Activator;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class VirtualMachinePool implements IVirtualMachinePool {

    final public static String Group = VirtualMachinePool.class.getName();
    final public static ConfigurationLocation UnusedVMKeepOn = new ConfigurationLocation(Group, "UnusedVMKeepOn");

    static {
        Activator.getWorkspace().addToConfigurations(UnusedVMKeepOn, "120000");
    }

    final static AtomicLong PooledVMID = new AtomicLong(0L);

    private class PooledVM implements Comparable<PooledVM> {

        final IVirtualMachine virtualMachine;
        final long killTime;
        final long id;

        public PooledVM(IVirtualMachine virtualMachine, long killTime) {
            this.virtualMachine = virtualMachine;
            this.killTime = killTime;
            this.id = PooledVMID.getAndIncrement();
        }

        @Override
        public int compareTo(PooledVM t) {
            int compare = new Long(killTime).compareTo(t.killTime);
            if (compare != 0) {
                return compare;
            }
            return new Long(id).compareTo(t.id);
        }
    }
    final VirtualMachineResource resource;
    final Map<Long, Future> pooledMachinKillers = new TreeMap<Long, Future>();
    final TreeSet<PooledVM> pool = new TreeSet<PooledVM>();
    final ScheduledExecutorService killer = Executors.newScheduledThreadPool(1);

    public VirtualMachinePool(VirtualMachineResource resource) {
        this.resource = resource;
    }

    @Override
    public IVirtualMachine borrowAVirtualMachine() throws InternalProcessingError, UserBadDataError {
        synchronized (pool) {
            if (!pool.isEmpty()) {
                PooledVM pooledVM = pool.pollFirst();
                pooledMachinKillers.get(pooledVM.id).cancel(true);
                return pooledVM.virtualMachine;
            }
        }
        return resource.launchAVirtualMachine();
    }

    @Override
    public void returnVirtualMachine(final IVirtualMachine virtualMachine) throws InternalProcessingError {
        long delay = Activator.getWorkspace().getPreferenceAsLong(UnusedVMKeepOn);
        long killTime = System.currentTimeMillis() + delay;

        final PooledVM pooledVM = new PooledVM(virtualMachine, killTime);
        synchronized (pool) {
            Future future = killer.schedule(new Runnable() {

                @Override
                public void run() {
                    synchronized (pool) {
                        virtualMachine.shutdown();
                        pool.remove(pooledVM);
                        pooledMachinKillers.remove(pooledVM.id);
                    }
                }
            }, delay, TimeUnit.MILLISECONDS);
            pooledMachinKillers.put(pooledVM.id, future);
        }
    }
}
