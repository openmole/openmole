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

import org.openmole.misc.exception.InternalProcessingError
import java.net.URI
import java.util.concurrent.atomic.AtomicLong
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.implementation.execution.Environment
import org.openmole.misc.workspace.ConfigurationLocation

import org.openmole.core.model.job.IJob
import org.openmole.misc.executorservice.ExecutorType
import org.openmole.misc.updater.Updater
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.pluginmanager.PluginManager
import org.openmole.misc.eventdispatcher.Event
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.core.model.execution.IEnvironment
import org.openmole.misc.tools.collection.OrderedSlidingList

object BatchEnvironment {
 
  trait Transfert {
    def id: Long
  }
  
  val transfertId = new AtomicLong
  
  case class BeginUpload(val id: Long, val file: File, val storage: Storage) extends Event[BatchEnvironment] with Transfert
  case class EndUpload(val id: Long, val file: File, val storage: Storage) extends Event[BatchEnvironment] with Transfert
  
  case class BeginDownload(val id: Long, val from: URI, val storage: Storage) extends Event[BatchEnvironment] with Transfert
  case class EndDownload(val id: Long, val from: URI, val storage: Storage) extends Event[BatchEnvironment] with Transfert
  
  def signalUpload[T](upload: => T, file: File, storage: Storage) = {
    val id = transfertId.getAndIncrement
    EventDispatcher.trigger(storage.environment, new BeginUpload(id, file, storage))
    try upload
    finally EventDispatcher.trigger(storage.environment, new EndUpload(id, file, storage))
  }
  
  def signalDownload[T](download: => T, from: URI, storage: Storage) = {
    val id = transfertId.getAndIncrement
    EventDispatcher.trigger(storage.environment, new BeginDownload(id, from, storage))
    try download
    finally EventDispatcher.trigger(storage.environment, new EndDownload(id, from, storage))
  }
  
  val MemorySizeForRuntime = new ConfigurationLocation("BatchEnvironment", "MemorySizeForRuntime")

  val RuntimeLocation = new ConfigurationLocation("BatchEnvironment", "RuntimeLocation")
    
  val JVMLinuxI386Location = new ConfigurationLocation("BatchEnvironment", "JVMLinuxI386Location")

  val JVMLinuxX64Location = new ConfigurationLocation("BatchEnvironment", "JVMLinuxX64Location")
  
  val MinValueForSelectionExploration = new ConfigurationLocation("BatchEnvironment", "MinValueForSelectionExploration")

  val QualityHysteresis = new ConfigurationLocation("BatchEnvironment", "QualityHysteresis")
  val CheckInterval = new ConfigurationLocation("BatchEnvironment", "CheckInterval")
  
  val CheckFileExistsInterval = new ConfigurationLocation("BatchEnvironment", "CheckFileExistsInterval")
  
  val MinUpdateInterval = new ConfigurationLocation("BatchEnvironment", "MinUpdateInterval")
  val MaxUpdateInterval = new ConfigurationLocation("BatchEnvironment", "MaxUpdateInterval")
  val IncrementUpdateInterval = new ConfigurationLocation("BatchEnvironment", "IncrementUpdateInterval");
  val StatisticsHistorySize = new ConfigurationLocation("Environment", "StatisticsHistorySize")

  Workspace += (MinUpdateInterval, "PT1M")
  Workspace += (MaxUpdateInterval, "PT20M")
  Workspace += (IncrementUpdateInterval, "PT1M")
  
  Workspace += (RuntimeLocation, () => new File(new File(Workspace.location, "runtime"), "org.openmole.runtime.tar.gz").getAbsolutePath)
  Workspace += (JVMLinuxI386Location, () => new File(new File(Workspace.location, "runtime"), "jvm-linux-i386.tar.gz").getAbsolutePath)
  Workspace += (JVMLinuxX64Location, () => new File(new File(Workspace.location, "runtime"), "jvm-linux-x64.tar.gz").getAbsolutePath)

  Workspace += (MemorySizeForRuntime, "512")
  Workspace += (QualityHysteresis, "100")
  Workspace += (CheckInterval, "PT2M")
  Workspace += (MinValueForSelectionExploration, "0.001")
  Workspace += (CheckFileExistsInterval, "PT1H")
  Workspace += (StatisticsHistorySize, "10000")
  
}

import BatchEnvironment._

abstract class BatchEnvironment extends Environment {
  
  val jobRegistry = new ExecutionJobRegistry
  val statistics = new OrderedSlidingList[StatisticSample](Workspace.preferenceAsInt(StatisticsHistorySize))
  
  AuthenticationRegistry.initAndRegisterIfNotAllreadyIs(authentication)
      
  def inMemorySizeForRuntime: Option[Int] = None
  
  val memorySizeForRuntime = inMemorySizeForRuntime match {
    case Some(mem) => mem
    case None => Workspace.preferenceAsInt(BatchEnvironment.MemorySizeForRuntime)
  }
  
  Updater.registerForUpdate(new BatchJobWatcher(this), ExecutorType.OWN)
    
  override def submit(job: IJob) = {
    val bej = executionJob(job)
    EventDispatcher.trigger(this, new IEnvironment.JobSubmitted(bej))
    Updater.delay(bej, ExecutorType.UPDATE)
    jobRegistry.register(bej)
  }
  
  def executionJob(job: IJob) = new BatchExecutionJob(this, job)
  
  def minUpdateInterval = Workspace.preferenceAsDurationInMs(MinUpdateInterval)
  def maxUpdateInterval = Workspace.preferenceAsDurationInMs(MaxUpdateInterval)
  def incrementUpdateInterval = Workspace.preferenceAsDurationInMs(IncrementUpdateInterval)
  
  @transient lazy val runtime = new File(Workspace.preference(BatchEnvironment.RuntimeLocation))
  @transient lazy val jvmLinuxI386 = new File(Workspace.preference(BatchEnvironment.JVMLinuxI386Location))
  @transient lazy val jvmLinuxX64 = new File(Workspace.preference(BatchEnvironment.JVMLinuxX64Location))

  @transient lazy val jobServices = {
    val jobServices = allJobServices
    if(jobServices.isEmpty) throw new InternalProcessingError("No job service available for the environment.")
    new JobServiceGroup(this, jobServices) 
  }
  
  @transient lazy val storages = {
    val storages = allStorages
    if(storages.isEmpty) throw new InternalProcessingError("No storage service available for the environment.")
    new StorageGroup(this, storages)
  }
  
  def allStorages: Iterable[Storage]
  def allJobServices: Iterable[JobService]
  
  def authentication: Authentication
  
  def selectAJobService: (JobService, AccessToken) = jobServices.selectAService

  def selectAStorage(usedFiles: Iterable[File]):  (Storage, AccessToken) = storages.selectAService(usedFiles)
  
  
  @transient private[environment] lazy val plugins = PluginManager.pluginsForClass(this.getClass)

}
