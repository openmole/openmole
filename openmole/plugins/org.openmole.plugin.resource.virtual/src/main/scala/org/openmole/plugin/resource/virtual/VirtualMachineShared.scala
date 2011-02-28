/*
 * Copyright (C) 2011 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.resource.virtual

import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.TimeUnit

class VirtualMachineShared(resource: VirtualMachineResource) extends AbstractVirtualMachinePool(resource) {

  var vm: IVirtualMachine = null
  val running = new AtomicBoolean(false)
  val used = new AtomicInteger(0)
    
  @transient var killerFuture: Future[_] = null

  @transient lazy val killer = Executors.newScheduledThreadPool(1)

  override def borrowAVirtualMachine: IVirtualMachine = synchronized {
       
    if (!running.get) {
      //Logger.getLogger(VirtualMachineShared.class.getName()).log(Level.FINE, "Starting VM.");
      vm = resource.launchAVirtualMachine
      running.set(true)
      //Logger.getLogger(VirtualMachineShared.class.getName()).log(Level.FINE, "VM started.");
    }

    used.incrementAndGet
            
    if(killerFuture != null) {
      killerFuture.cancel(true)
      killerFuture = null
    }      
    vm    
  }
    
  override def returnVirtualMachine(virtualMachine: IVirtualMachine) = synchronized {
    if (used.decrementAndGet == 0) {
      killerFuture = killer.schedule(new Runnable {

          override def run = {
            VirtualMachineShared.this.synchronized {
              //Logger.getLogger(VirtualMachineShared.class.getName()).log(Level.FINE, "Timeout on VM.");
              if (used.get == 0) {
                //Logger.getLogger(VirtualMachineShared.class.getName()).log(Level.FINE, "Shutting down vm.");
                vm.shutdown
                running.set(false)
                vm = null;
                //Logger.getLogger(VirtualMachineShared.class.getName()).log(Level.FINE, "VM shutted down.");
              }
            }
          }
        }, delay, TimeUnit.MILLISECONDS)
    }
  }
  
}

