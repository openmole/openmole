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

import java.util.Map
import java.util.TreeMap
import java.util.TreeSet
import java.util.concurrent.Future
import org.openmole.plugin.resource.virtual.internal.Activator
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.TimeUnit
import java.util.concurrent.Executors
import org.openmole.commons.aspect.caching.Cachable

class VirtualMachinePool(resource: VirtualMachineResource) extends IVirtualMachinePool {
    
  object PooledVMID {
    val id = new AtomicLong(0L)
  }

  private class PooledVM(val virtualMachine: IVirtualMachine, val killTime: Long, val id: Long = PooledVMID.id.getAndIncrement()) extends Comparable[PooledVM] {
    override def compareTo(t: PooledVM): Int = {
      val compare = killTime.compare(t.killTime)
      if (compare != 0) {
        return compare
      }
      long2Long(id).compareTo(t.id)
    }
  }
   
  private val pooledMachineKillers = new TreeMap[Long, Future[_]]()
  private val pool = new TreeSet[PooledVM]()
  
  @Cachable
  private def killer() = Executors.newScheduledThreadPool(1)

  override def borrowAVirtualMachine(): IVirtualMachine = {
      pool.synchronized {
        if (!pool.isEmpty) {
          val pooledVM = pool.pollFirst
          pooledMachineKillers.get(pooledVM.id).cancel(false)
          return pooledVM.virtualMachine
        }
      }
      return resource.launchAVirtualMachine
  }

  override def returnVirtualMachine(virtualMachine: IVirtualMachine) = {
        
    val killTime : Long = System.currentTimeMillis + delay

    val pooledVM = new PooledVM(virtualMachine, killTime)
    pool.synchronized {
      val future = killer.schedule(new Runnable() {

          override def run() = {
            pool.synchronized {
              if(pool.contains(pooledVM)) {
                pooledVM.virtualMachine.shutdown
                pool.remove(pooledVM)
                pooledMachineKillers.remove(pooledVM.id)
              }
            }
          }
        }, delay, TimeUnit.MILLISECONDS)
      pooledMachineKillers.put(pooledVM.id, future)
    }
  }
  

}
