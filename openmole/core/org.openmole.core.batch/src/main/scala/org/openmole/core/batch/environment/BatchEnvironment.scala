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
import org.openmole.core.batch.internal.Activator._
import org.openmole.core.implementation.execution.Environment
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.workspace.InteractiveConfiguration

import org.openmole.core.model.job.IJob
import org.openmole.misc.executorservice.ExecutorType

object BatchEnvironment {
  
  val MemorySizeForRuntime = new ConfigurationLocation("BatchEnvironment", "MemorySizeForRuntime")
    
  @InteractiveConfiguration(label = "Runtime location")
  val RuntimeLocation = new ConfigurationLocation("BatchEnvironment", "RuntimeLocation")
    
  val MinValueForSelectionExploration = new ConfigurationLocation("BatchEnvironment", "MinValueForSelectionExploration")

  val QualityHysteresis = new ConfigurationLocation("BatchEnvironment", "QualityHysteresis")
  val CheckInterval = new ConfigurationLocation("BatchEnvironment", "CheckInterval")
  
  val DataAllReadyPresentOnStoragePreference = new ConfigurationLocation("BatchEnvironment", "DataAllReadyPresentOnStoragePreference")
  
  workspace += (MemorySizeForRuntime, "256")
  workspace += (QualityHysteresis, "1000")
  workspace += (CheckInterval, "PT2M")
  workspace += (MinValueForSelectionExploration, "0.001")
  workspace += (DataAllReadyPresentOnStoragePreference, "10.0")
}


abstract class BatchEnvironment(inMemorySizeForRuntime: Option[Int]) extends Environment[BatchExecutionJob] {
  
  AuthenticationRegistry.initAndRegisterIfNotAllreadyIs(authenticationKey, authentication)
  
  @transient private lazy val _storagesLock = new ReentrantLock
  @transient private lazy val _jobServicesLock = new ReentrantLock
  
  @transient private lazy val _jobServices = new BatchJobServiceGroup(this)
  @transient private lazy val _storages = new BatchStorageGroup(this)
  
  val memorySizeForRuntime = inMemorySizeForRuntime match {
    case Some(mem) => mem
    case None => workspace.preferenceAsInt(BatchEnvironment.MemorySizeForRuntime)
  }
  
  updater.registerForUpdate(new BatchJobWatcher(this), ExecutorType.OWN)
    
  override def submit(job: IJob) = {
    val bej = new BatchExecutionJob(this, job, nextExecutionJobId)
    updater.delay(bej, ExecutorType.UPDATE)
    jobRegistry.register(bej)
  }
  
  @transient lazy val runtime: File = new File(workspace.preference(BatchEnvironment.RuntimeLocation))
  
  protected def selectStorages(storageGroup: BatchStorageGroup) = {
        
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

      executorService.executorService(ExecutorType.OWN).submit(r)
    }

    while (storageGroup.isEmpty && nbLeftRunning.get > 0) oneFinished.acquire
  
    if (storageGroup.isEmpty)  throw new InternalProcessingError("No storage available")
    
  }

  protected def selectWorkingJobServices(jobServiceGroup: BatchJobServiceGroup) = {
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

      executorService.executorService(ExecutorType.OWN).submit(test)
    }
    
    while (jobServiceGroup.isEmpty && nbStillRunning.get > 0) done.acquire

    if (jobServiceGroup.isEmpty) throw new InternalProcessingError("No job submission service available")

  }


  def jobServices: BatchJobServiceGroup = {
    _jobServicesLock.lock 
    try {
      if (_jobServices.isEmpty) selectWorkingJobServices(_jobServices)
      _jobServices
    } finally _jobServicesLock.unlock 
  }

  def storages: BatchStorageGroup = {
    _storagesLock.lock
    try {
      if (_storages.isEmpty)  selectStorages(_storages)
      _storages
    } finally _storagesLock.unlock
  }

  def allStorages: Iterable[BatchStorage]
  def allJobServices: Iterable[BatchJobService]
  
  def authenticationKey: BatchAuthenticationKey
  def authentication: BatchAuthentication
  
  def selectAJobService: (BatchJobService, AccessToken) = jobServices.selectAService

  def selectAStorage(usedFiles: Iterable[File]):  (BatchStorage, AccessToken) = storages.selectAService(usedFiles)

}
