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

package org.openmole.core.batch.environment


import java.util.concurrent.Semaphore
import java.util.concurrent.locks.ReentrantLock
import org.openmole.commons.aspect.eventdispatcher.IObjectListener
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.commons.tools.service.Priority
import org.openmole.commons.tools.service.RNG
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.control.BatchStorageControl
import org.openmole.core.batch.control.UsageControl
import org.openmole.core.batch.internal.Activator
import scala.collection.mutable.ArrayBuffer

class BatchStorageGroup(val expulseThreshold: Int) {
  class BatchRessourceGroupAdapterUsage extends IObjectListener[UsageControl] {
    override def eventOccured(obj: UsageControl) = waiting.release
  }
  
  private var resources = List[BatchStorage]()

  @transient lazy val waiting = new Semaphore(0)
  @transient lazy val selectingRessource = new ReentrantLock

  def selectAService: (BatchStorage, AccessToken) = {
    selectingRessource.lock
    try {
      var ret: (BatchStorage, AccessToken) = null
      do {
        //Select the less failing resources
        val resourcesCopy = synchronized {
          resources = resources.filter( {r =>
              BatchStorageControl.qualityControl(r.description) match {
                case Some(f) => f.failureRate <= expulseThreshold
                case None => true
              }
            })
          if(resources.isEmpty) throw new InternalProcessingError("No more reliable resource available.");

          resources
        }

        //Among them select one not over loaded
        val bestResourcesIt = resourcesCopy.iterator
        val notLoaded = new ArrayBuffer[(BatchStorage, AccessToken)]
        //var totalQuality = 0L
        
        while (bestResourcesIt.hasNext) {       
          val cur = bestResourcesIt.next

          BatchStorageControl.usageControl(cur.description).tryGetToken match {
            case None =>
            case Some(token) => notLoaded += ((cur, token))
          }
        }
             
        if (notLoaded.size > 0) {
          ret = notLoaded(RNG.nextInt(notLoaded.size))
          
          for (service <- notLoaded) {    
            if(service._1.description != ret._1.description) BatchStorageControl.usageControl(service._1.description).releaseToken(service._2) 
          }
        } else {
          //Logger.getLogger(getClass.getName).info("Waiting")
          waiting.acquire
        }
      } while (ret == null)
      return ret
    } finally {
      selectingRessource.unlock
    }
  }

  def +=(service: BatchStorage) = synchronized {

    resources :+= service
    val usageControl = BatchStorageControl.usageControl(service.description)
    Activator.getEventDispatcher.registerForObjectChangedSynchronous(usageControl, Priority.NORMAL, new BatchRessourceGroupAdapterUsage, UsageControl.ResourceReleased)
    
    waiting.release
  }
    

  def get(index: Int): BatchStorage = resources(index)

  def isEmpty: Boolean = resources.isEmpty

  def size: Int = resources.size

}
