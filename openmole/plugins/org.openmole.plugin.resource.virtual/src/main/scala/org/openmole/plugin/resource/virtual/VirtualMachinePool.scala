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

import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import scala.collection.immutable.TreeMap
import scala.collection.immutable.TreeSet


object VirtualMachinePool {
  private val ID = new AtomicLong(0L)
}

class VirtualMachinePool(resource: VirtualMachineResource) extends AbstractVirtualMachinePool(resource) {

  import VirtualMachinePool._
 
  private class PooledVM(val virtualMachine: IVirtualMachine, val killTime: Long) {

    val id = ID.getAndIncrement
  }

  implicit private def comparePooledVW = new Ordering[PooledVM] {
    override def compare(left: PooledVM, right: PooledVM): Int = {
      val compare = left.killTime.compareTo(right.killTime)
      if (compare != 0) return compare;
      return left.id.compareTo(right.id)
    }
  }
  
  private var pooledMachineKillers = new TreeMap[Long, Future[_]]
  private var pool = new TreeSet[PooledVM]

  @transient lazy val killer = Executors.newScheduledThreadPool(1)

  override def borrowAVirtualMachine: IVirtualMachine = {

    pool.synchronized {
      if (!pool.isEmpty) {
        val pooledVM = pool.head
        pool = pool.tail
        pooledMachineKillers.get(pooledVM.id).get.cancel(false)
        return pooledVM.virtualMachine
      }
    }
    return resource.launchAVirtualMachine
  }

  override def returnVirtualMachine(virtualMachine: IVirtualMachine) = {

    val killTime = System.currentTimeMillis + delay

    val pooledVM = new PooledVM(virtualMachine, killTime)
    
    pool.synchronized {
      val future = killer.schedule(new Runnable {

          override def run {
            pool.synchronized {
              if (pool.contains(pooledVM)) {
                pooledVM.virtualMachine.shutdown
                pool -= pooledVM
                pooledMachineKillers -= pooledVM.id
              }
            }
          }
        }, delay, TimeUnit.MILLISECONDS)
      
      pool.synchronized {
        pool += pooledVM
        pooledMachineKillers += pooledVM.id -> future
      }
    }
  }
}
