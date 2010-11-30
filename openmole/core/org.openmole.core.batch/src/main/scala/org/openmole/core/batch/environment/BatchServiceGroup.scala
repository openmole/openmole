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

package org.openmole.core.batch.environment

import java.util.ArrayList
import java.util.LinkedList
import java.util.concurrent.Semaphore
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

import java.util.logging.Logger
import org.openmole.commons.aspect.eventdispatcher.IObjectListener
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.commons.tools.service.RNG
import org.openmole.commons.tools.service.Priority

import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.control.BatchServiceControl
import org.openmole.core.batch.control.QualityControl
import org.openmole.core.batch.control.UsageControl
import org.openmole.core.batch.internal.Activator
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._

class BatchServiceGroup[T <: IBatchService[_,_]](expulseThreshold: Int) {

  class BatchRessourceGroupAdapterUsage extends IObjectListener[UsageControl] {
    override def eventOccured(obj: UsageControl) = waiting.release
  }
  
  private val resources = new LinkedList[T]

  @transient lazy val waiting: Semaphore = new Semaphore(0)
  @transient lazy val selectingRessource: Lock = new ReentrantLock

  def selectAService: (T, AccessToken) = {
    selectingRessource.lock
    try {
      var ret: (T, AccessToken) = null

      do {
        //Select the less failing resources
        val resourcesCopy = resources.synchronized {
          val resourcesIt = resources.iterator

          while (resourcesIt.hasNext) {
            val resource = resourcesIt.next

            BatchServiceControl.qualityControl(resource.description) match {
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
        val notLoaded = new ArrayBuffer[(T, AccessToken)]
        //var totalQuality = 0L
        
        while (bestResourcesIt.hasNext) {       
          val cur = bestResourcesIt.next

          BatchServiceControl.usageControl(cur.description).tryGetToken match {
            case None =>
            case Some(token) => notLoaded += ((cur, token))
//              val toInsert = ((cur, token, 
//                               Activator.getBatchRessourceControl.qualityControl(cur.description) match {
//                    case None => 1L
//                    case Some(quality) => 
//                      quality.quality
//                  }))
//              
//              notLoaded += toInsert
//              totalQuality += toInsert._3
          }
        }
             
        if (notLoaded.size > 0) {
          val selected = notLoaded(RNG.nextInt(notLoaded.size))
          
          for (service <- notLoaded) {    
            if(service._1.description != selected._1.description) BatchServiceControl.usageControl(service._1.description).releaseToken(service._2) 
          }
          selected
        } else {
          waiting.acquire
        }
      } while (ret == null)
      return ret
    } finally {
      selectingRessource.unlock
    }
  }

  def +=(service: T) = {
    resources.synchronized {
      resources.add(service)
      val usageControl = BatchServiceControl.usageControl(service.description)
      Activator.getEventDispatcher.registerForObjectChangedSynchronous(usageControl, Priority.NORMAL, new BatchRessourceGroupAdapterUsage, UsageControl.ResourceReleased)
    }
    waiting.release
  }
    
  def ++=(services: Iterable[T]) {
    resources.synchronized {
      for(service <- services) {
        this += service
      }
    }
  }

  def get(index: Int): T = {
    resources.synchronized {
      resources.get(index);
    }
  }

  def isEmpty: Boolean = resources.isEmpty

  def size: Int = resources.size

  def iterator: Iterator[T] = resources.iterator

}
