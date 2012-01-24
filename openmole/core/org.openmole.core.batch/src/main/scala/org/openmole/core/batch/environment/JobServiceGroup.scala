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
import org.openmole.misc.eventdispatcher.Event
import org.openmole.misc.eventdispatcher.EventListener
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.tools.service.Priority
import org.openmole.misc.tools.service.Random
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.control.JobServiceControl
import org.openmole.core.batch.control.UsageControl
import org.openmole.misc.workspace.Workspace
import scala.collection.mutable.ArrayBuffer

class JobServiceGroup(val environment: BatchEnvironment, resources: Iterable[JobService]) extends Iterable[JobService] {

  class BatchRessourceGroupAdapterUsage extends EventListener[UsageControl] {
    override def triggered(subMole: UsageControl, ev: Event[UsageControl]) = waiting.release
  }
  
  resources.foreach {
    service =>
    val usageControl = UsageControl.get(service.description)
    EventDispatcher.listen(usageControl, new BatchRessourceGroupAdapterUsage, classOf[UsageControl.ResourceReleased])
  }
  
  @transient lazy val waiting = new Semaphore(0)
  @transient lazy val selectingRessource = new ReentrantLock

  override def iterator = resources.iterator
  
  def selectAService: (JobService, AccessToken) = {
    if(resources.size == 1) {
      val r = resources.head
      return (r, UsageControl.get(r.description).waitAToken)
    } 

    selectingRessource.lock
    try {
      var ret: Option[(JobService, AccessToken)] = None

      do {
        val notLoaded = resources.flatMap {   
          cur =>
            UsageControl.get(cur.description).tryGetToken match {
              case None => None
              case Some(token) => 
                val quality = JobServiceControl.qualityControl(cur.description)
                val nbSubmitted = quality.submitted
                val fitness = (if(quality.submitted > 0) {
                  val v = math.pow((quality.runnig.toDouble / quality.submitted) * quality.successRate, 2)
                  val min = Workspace.preferenceAsDouble(BatchEnvironment.MinValueForSelectionExploration)
                  if(v < min) min else v
                } else math.pow(quality.successRate, 2)) 
  
                Some((cur, token, fitness))
            }   
        }

        if (!notLoaded.isEmpty) {
          var selected = Random.default.nextDouble * notLoaded.map{_._3}.sum
          
          for ((service, token, fitness) <- notLoaded) { 
            if(!ret.isDefined && selected <= fitness) ret = Some((service, token))
            else UsageControl.get(service.description).releaseToken(token) 
            selected -= fitness
          }
        } else waiting.acquire
        
      } while (!ret.isDefined)
      return ret.get
    } finally selectingRessource.unlock
    
  }

}
