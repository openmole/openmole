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

package org.openmole.runtime

import java.util.concurrent.Semaphore
import org.openmole.core.model.job.State.State
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.eventdispatcher.IObjectListenerWithArgs
import org.openmole.misc.tools.service.Priority
import org.openmole.misc.tools.service.Logger
import org.openmole.core.model.job.IMoleJob

object AllFinished extends Logger

class AllFinished extends IObjectListenerWithArgs[IMoleJob] {

  import AllFinished._
  
  val allFinished = new Semaphore(0)
  
  @volatile var nbJobs = 0
  @volatile var nbFinished = 0

  def registerJob(job: IMoleJob) = synchronized {
    allFinished.drainPermits
    nbJobs += 1
    EventDispatcher.registerForObjectChangedSynchronous(job, Priority.LOW, this, IMoleJob.StateChanged)
  }

  def waitAllFinished = {
    allFinished.acquire
    allFinished.release
  }

  override def eventOccured(job: IMoleJob, args: Array[Object]) = synchronized {
    val state = args(0).asInstanceOf[State]
    if (state.isFinal) {
      //logger.info("Job is finished " + job.id + ".")
      nbFinished += 1
      if (nbFinished >= nbJobs) allFinished.release
    }
  }
}
