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

import java.util.LinkedList
import java.util.concurrent.LinkedBlockingQueue
import java.util.logging.Logger
import org.openmole.core.implementation.execution.Environment
import org.openmole.core.implementation.internal.Activator
import org.openmole.core.implementation.job.Job
import org.openmole.core.model.execution.IExecutionJob
import org.openmole.core.model.execution.ExecutionState
import org.openmole.core.model.job.IJob
import org.openmole.core.model.job.IMoleJob
import org.openmole.misc.workspace.ConfigurationLocation

object LocalExecutionEnvironment extends Environment[IExecutionJob] {
  
  
  val DefaultNumberOfThreads = new ConfigurationLocation(LocalExecutionEnvironment.getClass.getSimpleName, "ThreadNumber")

  Activator.getWorkspace().addToConfigurations(DefaultNumberOfThreads, Integer.toString(1))
    
  private val jobs = new LinkedBlockingQueue[LocalExecutionJob]
  private val executers = new LinkedList[LocalExecuter]
  private var nbThreadVar = Activator.getWorkspace.getPreferenceAsInt(DefaultNumberOfThreads)
       
  addExecuters(nbThread)
    
  private[local] def addExecuters(nbExecuters: Int) = {
    for (i <- 0 until nbExecuters) {
      val executer = new LocalExecuter
      executers.synchronized {
        val thread = new Thread(executer)
        thread.setDaemon(true)
        thread.start
        executers.add(executer)
      }
    }
  }

  def nbThread: Int = nbThreadVar

  def setNbThread(newNbThread: Int) = {
    synchronized {
      if (nbThread != newNbThread) {
 
        if (newNbThread > nbThread) {
          addExecuters(newNbThread - nbThread)
        } else {
          var toStop = nbThread - newNbThread
          executers.synchronized {
            val it = executers.iterator

            while (it.hasNext && toStop > 0) {
              val exe = it.next
              if (!exe.stop) {
                exe.stop = true
                it.remove
                toStop -= 1
              }
            }
          }
        }

        nbThreadVar = newNbThread
      }
    }
  }

  override def submit(job: IJob) = {
    val ejob = new LocalExecutionJob(job, nextExecutionJobId)
    jobRegistry.register(ejob)
    submit(ejob)
  }

  def submit(moleJob: IMoleJob): Unit = {
    val job = new Job
    job += moleJob
    submit(job)
  }

  private def submit(ejob: LocalExecutionJob) = {
    ejob.state = ExecutionState.SUBMITED
    jobs.add(ejob)
  }

  private[local] def takeNextjob: LocalExecutionJob = jobs.take
  
   
}
