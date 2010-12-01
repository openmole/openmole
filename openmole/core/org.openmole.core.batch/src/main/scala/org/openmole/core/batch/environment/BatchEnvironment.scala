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

package org.openmole.core.batch.environment

import java.io.File
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger

import java.util.concurrent.locks.ReentrantLock
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.internal.Activator
import org.openmole.core.implementation.execution.Environment
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
   
  Activator.getWorkspace += (MemorySizeForRuntime, "512")
  Activator.getWorkspace += (ResourcesExpulseThreshod, "100")
  Activator.getWorkspace += (CheckInterval, "PT2M")
}


abstract class BatchEnvironment(inMemorySizeForRuntime: Option[Int]) extends Environment[BatchExecutionJob] {

  @transient lazy val _jobServices = new BatchJobServiceGroup(Activator.getWorkspace.preferenceAsInt(BatchEnvironment.ResourcesExpulseThreshod))
  @transient lazy val _storages = new BatchStorageGroup(Activator.getWorkspace.preferenceAsInt(BatchEnvironment.ResourcesExpulseThreshod))
  @transient lazy val _jsLock = new ReentrantLock
  @transient lazy val _storageLock = new ReentrantLock
  
  val memorySizeForRuntime = inMemorySizeForRuntime match {
    case Some(mem) => mem
    case None => Activator.getWorkspace.preferenceAsInt(BatchEnvironment.MemorySizeForRuntime)
  }
  
  Activator.getUpdater.registerForUpdate(new BatchJobWatcher(this), ExecutorType.OWN, Activator.getWorkspace.preferenceAsDurationInMs(BatchEnvironment.CheckInterval))
    
  override def submit(job: IJob) = {
    val bej = new BatchExecutionJob(this, job, nextExecutionJobId)

    Activator.getUpdater.delay(bej, ExecutorType.UPDATE)

    jobRegistry.register(bej)
  }
  
  @transient lazy val runtime: File = new File(Activator.getWorkspace.preference(BatchEnvironment.RuntimeLocation))
  
  protected def selectStorages(storages: BatchStorageGroup) = {
        
    val stors = allStorages

    val oneFinished = new Semaphore(0)
    val nbLeftRunning = new AtomicInteger(stors.size)

    for (storage <- stors) {
      val r = new Runnable {

        override def run = {
          try {
            if (storage.test)  storages += storage
          } finally {
            nbLeftRunning.decrementAndGet
            oneFinished.release
          }
        }
      }

      Activator.getExecutorService.getExecutorService(ExecutorType.OWN).submit(r)
    }

    while (storages.isEmpty && nbLeftRunning.get > 0) oneFinished.acquire
    
    if (storages.isEmpty)  throw new InternalProcessingError("No storage available")
    
  }

  protected def selectWorkingJobServices(jobServices: BatchJobServiceGroup) = {
    val allJobServicesList = allJobServices
    val done = new Semaphore(0)
    val nbStillRunning = new AtomicInteger(allJobServicesList.size)

    for (js <- allJobServicesList) {

      val test = new Runnable {

        override def run = {
          try {
            if (js.test) jobServices += js
          } finally {
            nbStillRunning.decrementAndGet
            done.release
          }
        }
      }

      Activator.getExecutorService.getExecutorService(ExecutorType.OWN).submit(test)
    }
    
    while (jobServices.isEmpty && nbStillRunning.get > 0) done.acquire

    if (jobServices.isEmpty) throw new InternalProcessingError("No job submission service available")

  }


  def jobServices: BatchJobServiceGroup = {
    _jsLock.lock
    try {
      if (_jobServices.isEmpty) selectWorkingJobServices(_jobServices)
      _jobServices
    } finally {
      _jsLock.unlock
    }
  }

  def storages: BatchStorageGroup = {
    _storageLock.lock
    try {
      if (_storages.isEmpty)  selectStorages(_storages)
      _storages
    } finally {
      _storageLock.unlock
    }
  }

  def allStorages: Iterable[BatchStorage]
  def allJobServices: Iterable[BatchJobService]
  
  def selectAJobService: (BatchJobService, AccessToken) = jobServices.selectAService

  def selectAStorage:  (BatchStorage, AccessToken) = storages.selectAService

}
