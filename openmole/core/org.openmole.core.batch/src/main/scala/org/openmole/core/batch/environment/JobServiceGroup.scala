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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.batch.environment

import java.util.concurrent.Semaphore
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Logger
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.eventdispatcher.IObjectListener
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.tools.service.Priority
import org.openmole.misc.tools.service.RNG
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.control.JobServiceControl
import org.openmole.core.batch.control.UsageControl
import org.openmole.misc.workspace.Workspace
import scala.collection.mutable.ArrayBuffer

class JobServiceGroup(val environment: BatchEnvironment) {

  class BatchRessourceGroupAdapterUsage extends IObjectListener[UsageControl] {
    override def eventOccured(obj: UsageControl) = waiting.release
  }
  
  private var resources = List[JobService]()

  @transient lazy val waiting = new Semaphore(0)
  @transient lazy val selectingRessource = new ReentrantLock

  def selectAService: (JobService, AccessToken) = {
    selectingRessource.lock
    try {
      var ret: (JobService, AccessToken) = null

      do {
        //Select the less failing resources
        //Select the less failing resources
        /*val resourcesCopy = synchronized {
         resources = resources.filter( {r =>
         JobServiceControl.qualityControl(r.description).failureRate <= expulseThreshold
         })
         if(resources.isEmpty) throw new InternalProcessingError("No more reliable resource available.")
         resources
         }*/
        val resourcesCopy = resources

        //Among them select one not over loaded
        val notLoaded = new ArrayBuffer[(JobService, AccessToken, Double)]
        var totalFitness = 0.
        
        for (cur <- resourcesCopy) {   
          
          JobServiceControl.usageControl(cur.description).tryGetToken match {
            case None =>
            case Some(token) => 
              val quality = JobServiceControl.qualityControl(cur.description)
              val nbSubmitted = quality.submitted
              val fitness = (if(quality.submitted > 0) {
                  val v = math.pow((quality.runnig.toDouble / quality.submitted) * quality.successRate, 2)
                  val min = Workspace.preferenceAsDouble(BatchEnvironment.MinValueForSelectionExploration)
                  //Logger.getLogger(getClass.getName).info("v = " + v + " ; " + "min = " + min)
                  if(v < min) min else v
                } else {
                  //Logger.getLogger(getClass.getName).info("sucess " + quality.successRate)
                  quality.successRate
                }) 
                
              
              //Logger.getLogger(getClass.getName).info("Fitness for " + cur.description + " " + fitness)
              
              notLoaded += ((cur, token, fitness))
              totalFitness += fitness
          }
          
        }
             
        //Logger.getLogger(getClass.getName).info("Not loaded " + notLoaded.size)
        if (notLoaded.size > 0) {
          var selected = RNG.nextDouble * totalFitness
          
          for (service <- notLoaded) { 
            //Logger.getLogger(getClass.getName).info("Not loaded test " + ret + " " + selected + " <= " + service._3)
            if(ret == null && selected <= service._3) ret = (service._1, service._2)
            else JobServiceControl.usageControl(service._1.description).releaseToken(service._2) 
            selected -= service._3
          }
        } else waiting.acquire
        
      } while (ret == null)
      return ret
    } finally {
      selectingRessource.unlock
    }
  }

  def +=(service: JobService) = {
    synchronized {
      resources :+= service
      val usageControl = JobServiceControl.usageControl(service.description)
      EventDispatcher.registerForObjectChangedSynchronous(usageControl, Priority.NORMAL, new BatchRessourceGroupAdapterUsage, UsageControl.ResourceReleased)
    }
    waiting.release
  }
  
  def ++=(services: Iterable[JobService]) = services.foreach{s => this += s}

  def isEmpty: Boolean = resources.isEmpty

  def size: Int = resources.size

}
