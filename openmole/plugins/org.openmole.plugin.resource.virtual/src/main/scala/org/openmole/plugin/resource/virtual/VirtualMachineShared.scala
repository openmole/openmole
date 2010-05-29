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

package org.openmole.plugin.resource.virtual

import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.Executors
import org.openmole.commons.aspect.caching.Cachable
import java.util.concurrent.TimeUnit

class VirtualMachineShared(resource: VirtualMachineResource) extends IVirtualMachinePool {

  var vm: IVirtualMachine = null
  val running = new AtomicBoolean(false)
  val used = new AtomicInteger(0)

  @Cachable
  private def killer() = Executors.newScheduledThreadPool(1)

  override def borrowAVirtualMachine(): IVirtualMachine = {
    synchronized {
      val ret: IVirtualMachine =  if(running.get()) vm
      else {
        running.set(true)
        vm = resource.launchAVirtualMachine
        vm
      }
      used.incrementAndGet
      return ret
    }
  }

  override def returnVirtualMachine(virtualMachine : IVirtualMachine) = {
    synchronized {
      if(used.decrementAndGet == 0) {
        killer.schedule(new Runnable() {

          override def run() = {
            VirtualMachineShared.this.synchronized {
              if(!running.get) {
                vm.shutdown
                running.set(false)
                vm = null
              }
            }
          }
        }, delay, TimeUnit.MILLISECONDS)
      }
    }
  }
}
