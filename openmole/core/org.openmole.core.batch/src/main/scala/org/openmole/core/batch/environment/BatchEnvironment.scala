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

import java.io.File
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger

import java.util.concurrent.locks.ReentrantLock
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.implementation.execution.Environment
import org.openmole.misc.workspace.ConfigurationLocation

import org.openmole.core.model.job.IJob
import org.openmole.misc.executorservice.ExecutorService
import org.openmole.misc.executorservice.ExecutorType
import org.openmole.misc.updater.internal.Updater
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.workspace.InteractiveConfiguration

object BatchEnvironment {
  
  val MemorySizeForRuntime = new ConfigurationLocation("BatchEnvironment", "MemorySizeForRuntime")
    
  @InteractiveConfiguration(label = "Runtime location")
  val RuntimeLocation = new ConfigurationLocation("BatchEnvironment", "RuntimeLocation")
    
  val MinValueForSelectionExploration = new ConfigurationLocation("BatchEnvironment", "MinValueForSelectionExploration")

  val QualityHysteresis = new ConfigurationLocation("BatchEnvironment", "QualityHysteresis")
  val CheckInterval = new ConfigurationLocation("BatchEnvironment", "CheckInterval")
  
  val DataAllReadyPresentOnStoragePreference = new ConfigurationLocation("BatchEnvironment", "DataAllReadyPresentOnStoragePreference")
  
  Workspace += (RuntimeLocation, () => new File(new File(Workspace.location, "runtime"), "org.openmole.runtime.tar.bz2").getAbsolutePath)
  Workspace += (MemorySizeForRuntime, "512")
  Workspace += (QualityHysteresis, "1000")
  Workspace += (CheckInterval, "PT2M")
  Workspace += (MinValueForSelectionExploration, "0.001")
  Workspace += (DataAllReadyPresentOnStoragePreference, "10.0")
}


abstract class BatchEnvironment(inMemorySizeForRuntime: Option[Int]) extends Environment {
  
  val jobRegistry = new ExecutionJobRegistry[BatchExecutionJob]
  
  AuthenticationRegistry.initAndRegisterIfNotAllreadyIs(authentication)
  
  @transient private lazy val _storagesLock = new ReentrantLock
  @transient private lazy val _jobServicesLock = new ReentrantLock
  
  @transient private lazy val _jobServices = new JobServiceGroup(this)
  @transient private lazy val _storages = new StorageGroup(this)
  
  val memorySizeForRuntime = inMemorySizeForRuntime match {
    case Some(mem) => mem
    case None => Workspace.preferenceAsInt(BatchEnvironment.MemorySizeForRuntime)
  }
  
  Updater.registerForUpdate(new BatchJobWatcher(this), ExecutorType.OWN)
    
  override def submit(job: IJob) = {
    val bej = new BatchExecutionJob(this, job, nextExecutionJobId)
    Updater.delay(bej, ExecutorType.UPDATE)
    jobRegistry.register(bej)
  }
  
  @transient lazy val runtime: File = new File(Workspace.preference(BatchEnvironment.RuntimeLocation))
  
  protected def selectStorages(storageGroup: StorageGroup) = {
        
    val stors = allStorages

    val oneFinished = new Semaphore(0)
    val nbLeftRunning = new AtomicInteger(stors.size)

    for (storage <- stors) {
      val r = new Runnable {

        override def run = {
          try {
            if (storage.test) storageGroup += storage
          } finally {
            nbLeftRunning.decrementAndGet
            oneFinished.release
          }
        }
      }

      ExecutorService.executorService(ExecutorType.OWN).submit(r)
    }

    while (storageGroup.isEmpty && nbLeftRunning.get > 0) oneFinished.acquire
  
    if (storageGroup.isEmpty)  throw new InternalProcessingError("No storage available")
    
  }

  /*protected def selectWorkingJobServices(jobServiceGroup: JobServiceGroup) = {
    val allJobServicesList = allJobServices
    val done = new Semaphore(0)
    val nbStillRunning = new AtomicInteger(allJobServicesList.size)

    for (js <- allJobServicesList) {

      val test = new Runnable {

        override def run = {
          try {
            if (js.test) jobServiceGroup += js
          } finally {
            nbStillRunning.decrementAndGet
            done.release
          }
        }
      }

      ExecutorService.executorService(ExecutorType.OWN).submit(test)
    }
    
    while (jobServiceGroup.isEmpty && nbStillRunning.get > 0) done.acquire

    if (jobServiceGroup.isEmpty) throw new InternalProcessingError("No job submission service available")

  }*/


  def jobServices: JobServiceGroup = {
    _jobServicesLock.lock 
    try {
      if (_jobServices.isEmpty) _jobServices ++= allJobServices
        //selectWorkingJobServices(_jobServices)
      _jobServices
    } finally _jobServicesLock.unlock 
  }

  def storages: StorageGroup = {
    _storagesLock.lock
    try {
      if (_storages.isEmpty) selectStorages(_storages)
      _storages
    } finally _storagesLock.unlock
  }

  def allStorages: Iterable[Storage]
  def allJobServices: Iterable[JobService]
  
  def authentication: BatchAuthentication
  
  def selectAJobService: (JobService, AccessToken) = jobServices.selectAService

  def selectAStorage(usedFiles: Iterable[File]):  (Storage, AccessToken) = storages.selectAService(usedFiles)

}
