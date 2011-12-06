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

package org.openmole.core.implementation.execution.local

import org.openmole.core.implementation.execution.StatisticRegistry
import org.openmole.core.implementation.execution.StatisticSample
import org.openmole.core.model.execution.ExecutionState
import org.openmole.core.model.execution.IExecutionJob
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.job.State
import org.openmole.core.model.task.IMoleTask
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.tools.service.Logger
import scala.collection.JavaConversions._

object LocalExecuter extends Logger

class LocalExecuter(environment: LocalExecutionEnvironment) extends Runnable {

  import LocalExecuter._
  
  var stop: Boolean = false;

  override def run = {
    
    while (!stop) {
      val executionJob = environment.takeNextjob
      try {
        val job = executionJob.job
        executionJob.state = ExecutionState.RUNNING
        val running = System.currentTimeMillis

        for (moleJob <- job.moleJobs) {
          if (moleJob.state != State.CANCELED) {
            if (classOf[IMoleTask].isAssignableFrom(moleJob.task.getClass)) jobGoneIdle
            moleJob.perform
            
            moleJob.exception match {
              case None =>
              case Some(e) => 
                EventDispatcher.trigger(moleJob, new IMoleJob.ExceptionRaised(e, SEVERE))
                //EventDispatcher.trigger(executionJob, new IExecutionJob.ExceptionRaised(e, SEVERE))
                logger.log(SEVERE, "Error in user job execution, job state is FAILED.", e)
            }
          }
        }
        executionJob.state = ExecutionState.DONE
        StatisticRegistry.sample(environment, job, new StatisticSample(executionJob.timeStamps.head.time, running, System.currentTimeMillis))
      } catch {
        case (e: InterruptedException) => if (!stop) logger.log(WARNING, "Interrupted despite stop is false.", e)  
        case e => logger.log(SEVERE, null, e)
      } finally executionJob.state = ExecutionState.KILLED
    }
  }

  def jobGoneIdle {
    environment.addExecuters(1)
    stop = true
  }
}
