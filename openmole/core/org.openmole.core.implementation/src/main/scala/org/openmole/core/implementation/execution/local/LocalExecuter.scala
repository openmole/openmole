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

package org.openmole.core.implementation.execution.local

import org.openmole.core.implementation.execution.StatisticSample
import org.openmole.core.model.execution.ExecutionState
import org.openmole.core.model.job.State
import java.util.logging.Level
import java.util.logging.Logger

import org.openmole.core.model.task.IMoleTask
import scala.collection.JavaConversions._

object LocalExecuter {
  val LOGGER = Logger.getLogger(LocalExecuter.getClass.getName) 
}


class LocalExecuter extends Runnable {

  import LocalExecuter. _
  
  var stop: Boolean = false;

  override def run = {
    
    while (!stop) {
      try {
        val executionJob = LocalExecutionEnvironment.takeNextjob
        val job = executionJob.job

        try {
          executionJob.state = ExecutionState.RUNNING
          val running = System.currentTimeMillis
          //executionJob.environment.sample(SampleType.WAITING, running - executionJob.creationTime, job)

          for (moleJob <- job.moleJobs) {
            if (moleJob.state != State.CANCELED) {
              if (classOf[IMoleTask].isAssignableFrom(moleJob.task.getClass)) {
                jobGoneIdle
              }
              
              moleJob.perform
              moleJob.finished(moleJob.context)
            }
          }
          executionJob.state = ExecutionState.DONE
          LocalExecutionEnvironment.sample(job, new StatisticSample(executionJob.creationTime, running, System.currentTimeMillis))

        } finally {
          LocalExecutionEnvironment.jobRegistry.removeJob(job);
        }
      } catch {
        case (e: InterruptedException) => 
          if (!stop) LOGGER.log(Level.WARNING, "Interrupted despite stop is false.", e)  
        case e => LOGGER.log(Level.SEVERE, null, e);
      }
    }
  }

  def jobGoneIdle {
    LocalExecutionEnvironment.addExecuters(1)
    stop = true
  }
}
