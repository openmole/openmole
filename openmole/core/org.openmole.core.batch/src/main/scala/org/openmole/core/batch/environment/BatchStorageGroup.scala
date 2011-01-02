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
import java.util.logging.Logger
import org.openmole.commons.aspect.eventdispatcher.IObjectListener
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.commons.tools.service.Priority
import org.openmole.commons.tools.service.RNG
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.control.BatchStorageControl
import org.openmole.core.batch.control.UsageControl
import org.openmole.core.batch.internal.Activator._
import scala.collection.mutable.ArrayBuffer

class BatchStorageGroup {
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
        val resourcesCopy = resources

        //Among them select one not over loaded
        val notLoaded = new ArrayBuffer[(BatchStorage, AccessToken, Double)]
        var totalFitness = 0.
        
        //Among them select one not over loaded
        val bestResourcesIt = resourcesCopy.iterator
        
        while (bestResourcesIt.hasNext) {       
          val cur = bestResourcesIt.next

          BatchStorageControl.usageControl(cur.description).tryGetToken match {
            case None =>
            case Some(token) => 
              val quality = BatchStorageControl.qualityControl(cur.description)
              val fitness = quality match {
                case Some(q) => 
                  val v = math.pow(1. * q.success, 2)
                  val min = workspace.preferenceAsDouble(BatchEnvironment.MinValueForSelectionExploration)
                  if(v < min) min else v
                case None => 1.
              }
              
              Logger.getLogger(getClass.getName).info("Fitness for " + cur.description + " " + fitness)

              notLoaded += ((cur, token, fitness))
              totalFitness += fitness
          }
        }
             
        if (notLoaded.size > 0) {
          var selected = RNG.nextDouble * totalFitness
          
          for (service <- notLoaded) {    
            if(ret == null && selected <= service._3) ret = (service._1, service._2)
            else BatchStorageControl.usageControl(service._1.description).releaseToken(service._2) 
            selected -= service._3
          }
        } else {
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
    eventDispatcher.registerForObjectChangedSynchronous(usageControl, Priority.NORMAL, new BatchRessourceGroupAdapterUsage, UsageControl.ResourceReleased)
    
    waiting.release
  }
    

  def get(index: Int): BatchStorage = resources(index)

  def isEmpty: Boolean = resources.isEmpty

  def size: Int = resources.size

}
