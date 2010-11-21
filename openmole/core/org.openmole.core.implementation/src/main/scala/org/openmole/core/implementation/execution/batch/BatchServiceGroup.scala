/*
  * Copyright (C) 2010 reuillon
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as published by
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

import org.openmole.commons.aspect.eventdispatcher.IObjectListener
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.commons.tools.service.RNG
import org.openmole.commons.tools.service.Priority

import org.openmole.core.batchservicecontrol.IQualityControl
import org.openmole.core.batchservicecontrol.IUsageControl
import org.openmole.core.model.execution.batch.IBatchServiceGroup
import org.openmole.core.implementation.internal.Activator
import org.openmole.core.model.execution.batch.IAccessToken
import org.openmole.core.model.execution.batch.IBatchService
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._

class BatchServiceGroup[T <: IBatchService[_,_]](expulseThreshold: Int) extends IBatchServiceGroup[T] {

  class BatchRessourceGroupAdapterUsage extends IObjectListener[IUsageControl] {
    override def eventOccured(obj: IUsageControl) = waiting.release
  }
  
  private val resources = new LinkedList[T]

  @transient lazy val waiting: Semaphore = new Semaphore(0)
  @transient lazy val selectingRessource: Lock = new ReentrantLock

  override def selectAService: (T, IAccessToken) = {
    selectingRessource.lock
    try {
      var ret: (T, IAccessToken) = null

      while (ret == null) {
        //Select the less failing resources
        val resourcesCopy = resources.synchronized {
          val resourcesIt = resources.iterator

          while (resourcesIt.hasNext) {
            val resource = resourcesIt.next

            Activator.getBatchRessourceControl.qualityControl(resource.description) match {
              case Some(f) => if(f.failureRate > expulseThreshold) resourcesIt.remove
              case None => 
            }
          }

          if(resources.isEmpty) throw new InternalProcessingError("No more reliable resource available.");
                    
          val ret = new ArrayList[T](resources.size)
          ret.addAll(resources)
          ret
        }

        //Among them select one not over loaded
        val bestResourcesIt = resourcesCopy.iterator
        val notLoaded = new ArrayBuffer[(T, IAccessToken, Long)]
        var totalQuality = 0L
        
        while (bestResourcesIt.hasNext) {       
          val cur = bestResourcesIt.next

            Activator.getBatchRessourceControl.usageControl(cur.description).tryGetToken match {
              case None =>
              case Some(token) =>
                val toInsert = ((cur, token, 
                 Activator.getBatchRessourceControl.qualityControl(cur.description) match {
                  case None => 1L
                  case Some(quality) => quality.quality
                }))
                notLoaded += toInsert
                totalQuality += toInsert._3
            }
        }
               
        if (notLoaded.size > 0) {
          var selectedIndex = RNG.nextLong(totalQuality)
          val it = notLoaded.iterator
          
          var selected = it.next
          
          while(selectedIndex >= selected._3) {
            selectedIndex -= selected._3
            selected = it.next
          }
          
          for (service <- notLoaded) {    
            if(service._1.description != selected._1.description) Activator.getBatchRessourceControl.usageControl(service._1.description).releaseToken(service._2) 
          }
          ret = (selected._1, selected._2)
        } else {
          waiting.acquire
        }
      }
      return ret
    } finally {
      selectingRessource.unlock
    }
  }

  override def add(service: T) = {
    resources.synchronized {
      resources.add(service)
      val usageControl = Activator.getBatchRessourceControl.usageControl(service.description)
      Activator.getEventDispatcher.registerForObjectChangedSynchronous(usageControl, Priority.NORMAL, new BatchRessourceGroupAdapterUsage, IUsageControl.ResourceReleased)
    }
    waiting.release
  }
    
  override def addAll(services: Iterable[T]) {
    resources.synchronized {
      for(service <- services) {
        add(service)
      }
    }
  }

  def get(index: Int): T = {
    resources.synchronized {
      resources.get(index);
    }
  }

  override def isEmpty: Boolean = resources.isEmpty

  override def size: Int = resources.size

  override def iterator: Iterator[T] = resources.iterator

}
