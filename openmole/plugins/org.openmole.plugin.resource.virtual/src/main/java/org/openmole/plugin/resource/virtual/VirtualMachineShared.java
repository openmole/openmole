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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.openmole.commons.aspect.caching.Cachable;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class VirtualMachineShared extends AbstractVirtualMachinePool {

    IVirtualMachine vm = null;
    final AtomicBoolean running = new AtomicBoolean(false);
    final AtomicInteger used = new AtomicInteger(0);

    public VirtualMachineShared(VirtualMachineResource resource) {
        super(resource);
    }

    @Cachable
    private ScheduledExecutorService killer() {
        return Executors.newScheduledThreadPool(1);
    }

    @Override
    public IVirtualMachine borrowAVirtualMachine() throws UserBadDataError, InternalProcessingError, InterruptedException {
        synchronized (this) {
            if (!running.get()) {
                vm = resource().launchAVirtualMachine();
                running.set(true);
            }

            used.incrementAndGet();
            return vm;
        }
    }

    @Override
    public void returnVirtualMachine(IVirtualMachine virtualMachine) throws InternalProcessingError {
        synchronized (this) {
            if (used.decrementAndGet() == 0) {
                killer().schedule(new Runnable() {

                    @Override
                    public void run() {
                        synchronized (VirtualMachineShared.this) {
                            if (used.get() == 0) {
                                vm.shutdown();
                                running.set(false);
                                vm = null;
                            }
                        }
                    }
                }, delay(), TimeUnit.MILLISECONDS);
            }
        }
    }
}
