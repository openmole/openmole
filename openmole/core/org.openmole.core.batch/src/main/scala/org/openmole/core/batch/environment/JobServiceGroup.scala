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
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.eventdispatcher.Event
import org.openmole.misc.eventdispatcher.EventListener
import org.openmole.misc.tools.service.Random
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.control.JobServiceControl
import org.openmole.core.batch.control.UsageControl
import ServiceGroup._

class JobServiceGroup(val environment: BatchEnvironment, resources: Iterable[JobService]) extends ServiceGroup with Iterable[JobService] {

  class BatchRessourceGroupAdapterUsage extends EventListener[UsageControl] {
    override def triggered(subMole: UsageControl, ev: Event[UsageControl]) = waiting.release
  }

  resources.foreach {
    service ⇒
    val usageControl = UsageControl.get(service.description)
    EventDispatcher.listen(usageControl, new BatchRessourceGroupAdapterUsage, classOf[UsageControl.ResourceReleased])
  }

  @transient lazy val waiting = new Semaphore(0)
  @transient lazy val selectingRessource = new ReentrantLock

  override def iterator = resources.iterator

  def selectAService: (JobService, AccessToken) = {
    if (resources.size == 1) {
      val r = resources.head
      return (r, UsageControl.get(r.description).waitAToken)
    }

    selectingRessource.lock
    try {
      var ret: Option[(JobService, AccessToken)] = None

      do {
        val usable = 
          for(
            r <- resources;
            token <- UsageControl.get(r.description).tryGetToken
          ) yield (r, token, JobServiceControl.qualityControl(r.description))
       
        val maxDone = usable.map{ case (_, _ , q) => q.done }.max.toDouble
        
        val notLoaded = 
          for((r, t, q) <- usable) yield {
            val nbSubmitted = q.submitted
            val maxDoneRatio = math.pow(if(maxDone == 0) 1 else q.done / maxDone, 2)
            val fitness = orMin (
              if (q.submitted > 0) 
                math.pow((q.runnig.toDouble / q.submitted) * q.successRate * maxDoneRatio, 2)
              else math.pow(q.successRate, 3)
            )
            (r, t, fitness)
          }
        
        if (!notLoaded.isEmpty) {
          var selected = Random.default.nextDouble * notLoaded.map { _._3 }.sum

          for ((service, token, fitness) ← notLoaded) {
            if (!ret.isDefined && selected <= fitness) ret = Some((service, token))
            else UsageControl.get(service.description).releaseToken(token)
            selected -= fitness
          }
        } else waiting.acquire

      } while (!ret.isDefined)
      return ret.get
    } finally selectingRessource.unlock

  }

}
