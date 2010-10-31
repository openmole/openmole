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

package org.openmole.core.implementation.execution.batch

import java.io.File
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Lock

import org.openmole.core.model.execution.batch.IBatchExecutionJob
import org.openmole.core.model.execution.batch.IAccessToken
import org.openmole.core.model.execution.batch.IBatchEnvironment
import org.openmole.core.model.execution.batch.IBatchJobService
import org.openmole.core.model.execution.batch.IBatchServiceGroup
import org.openmole.core.model.execution.batch.IBatchStorage

import java.util.concurrent.locks.ReentrantLock
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.core.implementation.execution.Environment
import org.openmole.core.implementation.internal.Activator
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.workspace.InteractiveConfiguration

import org.openmole.core.model.job.IJob
import org.openmole.misc.executorservice.ExecutorType

object BatchEnvironment {
  
  val MemorySizeForRuntime = new ConfigurationLocation("BatchEnvironment", "MemorySizeForRuntime")
    
  @InteractiveConfiguration(label = "Runtime location")
  val RuntimeLocation = new ConfigurationLocation("BatchEnvironment", "RuntimeLocation")
  
  val ResourcesExpulseThreshod = new ConfigurationLocation("BatchEnvironment", "ResourcesExpulseThreshod")
  val CheckInterval = new ConfigurationLocation("BatchEnvironment", "CheckInterval")

  Activator.getWorkspace.addToConfigurations(MemorySizeForRuntime, "512")
  Activator.getWorkspace.addToConfigurations(ResourcesExpulseThreshod, "100")
  Activator.getWorkspace.addToConfigurations(CheckInterval, "PT2M")
    
}


abstract class BatchEnvironment[JS <: IBatchJobService[_,_]](inMemorySizeForRuntime: Option[Int]) extends Environment[IBatchExecutionJob[_]] with IBatchEnvironment[JS] {

  @transient
  var jobServices: BatchServiceGroup[JS] = null
    
  @transient
  var storages: BatchServiceGroup[IBatchStorage[_,_]] = null
    
  @transient
  var initJS: Lock = null
  
  @transient
  var initST: Lock = null
  

  val memorySizeForRuntime = inMemorySizeForRuntime match {
    case Some(mem) => mem
    case None => Activator.getWorkspace.getPreferenceAsInt(BatchEnvironment.MemorySizeForRuntime)
  }
  
  Activator.getUpdater.registerForUpdate(new BatchJobWatcher(this), ExecutorType.OWN, Activator.getWorkspace().getPreferenceAsDurationInMs(BatchEnvironment.CheckInterval));
    
  override def submit(job: IJob) = {
    val bej = new BatchExecutionJob[JS](this, job, nextExecutionJobId)

    Activator.getUpdater.delay(bej, ExecutorType.UPDATE)

    jobRegistry.register(bej)
  }
  
  override def runtime: File = {
    new File(Activator.getWorkspace.getPreference(BatchEnvironment.RuntimeLocation))
  }

  protected def selectStorages(storages: BatchServiceGroup[IBatchStorage[_,_]]) = {
        
    val stors = allStorages

    val oneFinished = new Semaphore(0)
    val nbLeftRunning = new AtomicInteger(stors.size)

    for (storage <- stors) {
      val r = new Runnable {

        override def run = {
          try {
            if (storage.test) {
              storages.add(storage)
            }
          } finally {
            nbLeftRunning.decrementAndGet
            oneFinished.release
          }
        }
      }

      Activator.getExecutorService.getExecutorService(ExecutorType.OWN).submit(r)
    }

    while ((storages.isEmpty) && nbLeftRunning.get > 0) {
      oneFinished.acquire
    }

    if (storages.isEmpty) {
      throw new InternalProcessingError("No storage available")
    }
  }

  protected def selectWorkingJobServices(jobServices: BatchServiceGroup[JS]) = {
    val allJobServicesList = allJobServices
    val done = new Semaphore(0)
    val nbStillRunning = new AtomicInteger(allJobServicesList.size)

    for (js <- allJobServicesList) {

      val test = new Runnable {

        override def run = {
          try {
            if (js.test) {
              jobServices.add(js)
            }
          } finally {
            nbStillRunning.decrementAndGet
            done.release
          }
        }
      }

      Activator.getExecutorService().getExecutorService(ExecutorType.OWN).submit(test)
    }


    while (jobServices.isEmpty && nbStillRunning.get > 0) {
      done.acquire
    }

    if (jobServices.isEmpty) {
      throw new InternalProcessingError("No job submission service available");
    }

  }

  override def getAStorage:  (IBatchStorage[_,_], IAccessToken) = {
    getStorages.getAService
  }

  override def getJobServices: IBatchServiceGroup[JS] = {
    getInitJS.lock
    try {
      if(jobServices == null) {
        jobServices = new BatchServiceGroup[JS](Activator.getWorkspace.getPreferenceAsInt(BatchEnvironment.ResourcesExpulseThreshod))
      }
            
      if (jobServices.isEmpty) {
        selectWorkingJobServices(jobServices)
      }
      return jobServices;
    } finally {
      getInitJS.unlock
    }
  }

  override def getStorages: IBatchServiceGroup[IBatchStorage[_,_]] = {
    getInitST.lock
    try {
      if(storages == null) {
        storages = new BatchServiceGroup[IBatchStorage[_,_]](Activator.getWorkspace().getPreferenceAsInt(BatchEnvironment.ResourcesExpulseThreshod))
      }

      if (storages.isEmpty) {
        selectStorages(storages)
      }
      return storages
    } finally {
      getInitST.unlock
    }
  }

  override def getAJobService: (JS, IAccessToken) = {
    getJobServices.getAService
  }

  private def getInitJS: Lock = {
    if (initJS != null) {
      return initJS
    }

    synchronized {
      if (initJS == null) {
        initJS = new ReentrantLock
      }
      return initJS
    }
  }

  private def getInitST: Lock = {
    if (initST != null) {
      return initST
    }

    synchronized {
      if (initST == null) {
        initST = new ReentrantLock
      }
      return initST
    }
  }
}
