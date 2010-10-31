/*
 * Copyright (C) 2010 reuillon
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

package org.openmole.core.implementation.execution.batch

import java.util.ArrayList
import java.util.LinkedList
import java.util.concurrent.Semaphore
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import org.openmole.commons.aspect.eventdispatcher.IObjectChangedSynchronousListener

import org.openmole.commons.exception.InternalProcessingError
import org.openmole.commons.tools.service.RNG
import org.openmole.commons.tools.service.Priority

import org.openmole.core.batchservicecontrol.IUsageControl
import org.openmole.core.model.execution.batch.IBatchServiceGroup
import org.openmole.core.implementation.internal.Activator
import org.openmole.core.model.execution.batch.IAccessToken
import org.openmole.core.model.execution.batch.IBatchService
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._

class BatchServiceGroup[T <: IBatchService[_,_]](expulseThreshold: Int) extends IBatchServiceGroup[T] {

  class BatchRessourceGroupAdapter extends IObjectChangedSynchronousListener[IUsageControl] {

    override def objectChanged(obj: IUsageControl) = {
      ressourceTokenReleased
    }
        
  }

  private val resources = new LinkedList[T]

  @transient
  var waiting: Semaphore = null
    
  @transient
  var selectingRessource: Lock = null


  private def getWaiting: Semaphore = {
    synchronized {
      if (waiting == null) {
        waiting = new Semaphore(0)
      }
      return waiting
    }
  }

  private def waitForRessourceReleased = {
    getWaiting.acquire
  }

  override def getAService: (T, IAccessToken) = {

    getSelectingRessource.lock
    try {
      var ret: (T, IAccessToken) = null;

      while (ret == null) {

        //Select the less failing resources
        val resourcesCopy = resources.synchronized {

          val resourcesIt = resources.iterator

          while (resourcesIt.hasNext) {
            val resource = resourcesIt.next

            if (Activator.getBatchRessourceControl.getController(resource.description).getFailureControl().getFailureRate() > expulseThreshold) {
              resourcesIt.remove
            }
          }

          if(resources.isEmpty) throw new InternalProcessingError("No more reliable resource available.");
                    
          val ret = new ArrayList[T](resources.size)
          ret.addAll(resources)
          ret
        }

        //Among them select one not over loaded
        val bestResourcesIt = resourcesCopy.iterator
        val notLoaded = new ArrayList[(T, IAccessToken)]

        while (bestResourcesIt.hasNext()) {
                    
          val cur = bestResourcesIt.next();

          val token = Activator.getBatchRessourceControl().getController(cur.description).getUsageControl().getAccessTokenInterruptly

          if (token != null) {
            notLoaded.add((cur, token));
          }
        }
               
        if (notLoaded.size > 0) {
          ret = notLoaded.remove(RNG.getRng().nextInt(notLoaded.size()));

          for (other <- notLoaded) {                    
            Activator.getBatchRessourceControl().getController(other._1.description).getUsageControl().releaseToken(other._2) 
          }
        } else {
          waitForRessourceReleased
        }

      }
      return ret
    } finally {
      getSelectingRessource.unlock
    }
  }

  override def add(service: T) = {
    resources.synchronized {
      resources.add(service)
      val usageControl = Activator.getBatchRessourceControl.getController(service.description).getUsageControl
      Activator.getEventDispatcher().registerListener(usageControl, Priority.NORMAL.getValue, new BatchRessourceGroupAdapter, IUsageControl.resourceReleased)
    }
    ressourceTokenReleased
  }
    
  override def addAll(services: Iterable[T]) {
    resources.synchronized {
      for(service <- services) {
        add(service);
      }
    }
  }
    
    

  def get(index: Int): T = {
    resources.synchronized {
      resources.get(index);
    }
  }

  override def isEmpty: Boolean = {
    resources.isEmpty
  }

  override def size: Int = {
    resources.size
  }

  override def iterator: Iterator[T] = {
    resources.iterator
  }

  private def  getSelectingRessource: Lock = {
    if (selectingRessource != null) {
      return selectingRessource
    }
    synchronized {
      if (selectingRessource == null) {
        selectingRessource = new ReentrantLock
      }
    }
    selectingRessource
  }


  private def ressourceTokenReleased = {
    getWaiting.release
  }
}
