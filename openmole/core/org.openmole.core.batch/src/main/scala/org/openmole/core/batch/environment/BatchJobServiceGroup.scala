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
import org.openmole.core.batch.control.BatchJobServiceControl
import org.openmole.core.batch.control.UsageControl
import org.openmole.core.batch.internal.Activator
import scala.collection.mutable.ArrayBuffer

class BatchJobServiceGroup(val expulseThreshold: Int) {

  class BatchRessourceGroupAdapterUsage extends IObjectListener[UsageControl] {
    override def eventOccured(obj: UsageControl) = waiting.release
  }
  
  private var resources = List[BatchJobService]()

  @transient lazy val waiting = new Semaphore(0)
  @transient lazy val selectingRessource = new ReentrantLock

  def selectAService: (BatchJobService, AccessToken) = {
    selectingRessource.lock
    try {
      var ret: (BatchJobService, AccessToken) = null

      do {
        //Select the less failing resources
        //Select the less failing resources
        val resourcesCopy = synchronized {
          resources = resources.filter( {r =>
              BatchJobServiceControl.qualityControl(r.description).failureRate <= expulseThreshold
            })
          if(resources.isEmpty) throw new InternalProcessingError("No more reliable resource available.")
          resources
        }

        //Among them select one not over loaded
        val notLoaded = new ArrayBuffer[(BatchJobService, AccessToken, Double)]
        var totalFitness = 0.
        
        for (cur <- resourcesCopy) {       
          BatchJobServiceControl.usageControl(cur.description).tryGetToken match {
            case None =>
            case Some(token) => 
              val quality = BatchJobServiceControl.qualityControl(cur.description)
              val nbSubmitted = quality.submitted
              val fitness = if(quality.submitted > 0) {
                math.pow(quality.runnig.toDouble / quality.submitted, 2)
              } else Double.PositiveInfinity
              
              //Logger.getLogger(getClass.getName).info("Fitness for " + cur.description + " " + fitness)
              
              notLoaded += ((cur, token, fitness))
              totalFitness += fitness
          }
          
        }
             
       if (notLoaded.size > 0) {
         var selected = RNG.nextDouble * totalFitness
          
          for (service <- notLoaded) {    
            if(ret == null && selected <= service._3) ret = (service._1, service._2)
            else BatchJobServiceControl.usageControl(service._1.description).releaseToken(service._2) 
            selected -= service._3
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

  def +=(service: BatchJobService) = {
    synchronized {
      resources :+= service
      val usageControl = BatchJobServiceControl.usageControl(service.description)
      Activator.getEventDispatcher.registerForObjectChangedSynchronous(usageControl, Priority.NORMAL, new BatchRessourceGroupAdapterUsage, UsageControl.ResourceReleased)
    }
    waiting.release
  }

  def isEmpty: Boolean = resources.isEmpty

  def size: Int = resources.size

}
