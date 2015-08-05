/*
 * Copyright (C) 2010 Romain Reuillon
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
import org.openmole.core.event.{ Event, EventDispatcher }
import java.util.concurrent.atomic.AtomicLong
import org.openmole.core.batch.control._
import org.openmole.core.batch.storage._
import org.openmole.core.batch.jobservice._
import org.openmole.core.batch.refresh._
import org.openmole.core.batch.replication._
import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.fileservice.FileService
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.serializer.SerialiserService
import org.openmole.tool.file._
import org.openmole.tool.logger.Logger
import org.openmole.tool.thread._
import org.openmole.core.updater.Updater
import org.openmole.core.workflow.job._
import org.openmole.core.workspace.{ Workspace, ConfigurationLocation }
import org.openmole.core.workflow.execution._
import org.openmole.core.batch.message._
import org.openmole.tool.hash.Hash
import ref.WeakReference
import scala.Predef.Set
import scala.collection.mutable.{ MultiMap, Set, HashMap }

object BatchEnvironment extends Logger {

  trait Transfer {
    def id: Long
  }

  val transferId = new AtomicLong

  case class BeginUpload(id: Long, file: File, path: String, storage: StorageService) extends Event[BatchEnvironment] with Transfer
  case class EndUpload(id: Long, file: File, path: String, storage: StorageService) extends Event[BatchEnvironment] with Transfer

  case class BeginDownload(id: Long, file: File, path: String, storage: StorageService) extends Event[BatchEnvironment] with Transfer
  case class EndDownload(id: Long, file: File, path: String, storage: StorageService) extends Event[BatchEnvironment] with Transfer

  def signalUpload[T](upload: ⇒ T, file: File, path: String, storage: StorageService) = {
    val id = transferId.getAndIncrement
    EventDispatcher.trigger(storage.environment, new BeginUpload(id, file, path, storage))
    try upload
    finally EventDispatcher.trigger(storage.environment, new EndUpload(id, file, path, storage))
  }

  def signalDownload[T](download: ⇒ T, path: String, storage: StorageService, file: File) = {
    val id = transferId.getAndIncrement
    EventDispatcher.trigger(storage.environment, new BeginDownload(id, file, path, storage))
    try download
    finally EventDispatcher.trigger(storage.environment, new EndDownload(id, file, path, storage))
  }

  val MemorySizeForRuntime = new ConfigurationLocation("BatchEnvironment", "MemorySizeForRuntime")

  val CheckInterval = new ConfigurationLocation("BatchEnvironment", "CheckInterval")

  val CheckFileExistsInterval = new ConfigurationLocation("BatchEnvironment", "CheckFileExistsInterval")

  val MinUpdateInterval = new ConfigurationLocation("BatchEnvironment", "MinUpdateInterval")
  val MaxUpdateInterval = new ConfigurationLocation("BatchEnvironment", "MaxUpdateInterval")
  val IncrementUpdateInterval = new ConfigurationLocation("BatchEnvironment", "IncrementUpdateInterval")
  val MaxUpdateErrorsInARow = ConfigurationLocation("BatchEnvironment", "MaxUpdateErrorsInARow")

  val JobManagementThreads = new ConfigurationLocation("BatchEnvironment", "JobManagementThreads")

  val StoragesGCUpdateInterval = new ConfigurationLocation("BatchEnvironment", "StoragesGCUpdateInterval")

  val NoTokenForServiceRetryInterval = new ConfigurationLocation("BatchEnvironment", "NoTokenForServiceRetryInterval")

  val RuntimeMemoryMargin = ConfigurationLocation("BatchEnvironment", "RuntimeMemoryMargin")

  Workspace += (MinUpdateInterval, "PT1M")
  Workspace += (MaxUpdateInterval, "PT10M")
  Workspace += (IncrementUpdateInterval, "PT1M")
  Workspace += (MaxUpdateErrorsInARow, "3")

  private def runtimeDirLocation = Workspace.openMOLELocation.getOrElse(throw new InternalProcessingError("openmole.location not set"))./("runtime")

  @transient lazy val runtimeLocation = runtimeDirLocation./("runtime.tar.gz")
  @transient lazy val JVMLinuxI386Location = runtimeDirLocation./("jvm-386.tar.gz")
  @transient lazy val JVMLinuxX64Location = runtimeDirLocation./("jvm-x64.tar.gz")

  Workspace += (MemorySizeForRuntime, "1024")
  Workspace += (CheckInterval, "PT1M")
  Workspace += (CheckFileExistsInterval, "PT1H")
  Workspace += (JobManagementThreads, "200")

  Workspace += (StoragesGCUpdateInterval, "PT1H")
  Workspace += (NoTokenForServiceRetryInterval, "PT2M")

  Workspace += (RuntimeMemoryMargin, "400")

  def defaultRuntimeMemory = Workspace.preferenceAsInt(BatchEnvironment.MemorySizeForRuntime)

  lazy val jobManager = new JobManager
}

import BatchEnvironment._

trait BatchExecutionJob extends ExecutionJob { bej ⇒
  def job: Job
  var serializedJob: Option[SerializedJob] = None
  var batchJob: Option[BatchJob] = None

  def moleJobs = job.moleJobs
  def runnableTasks = job.moleJobs.map(RunnableTask(_))

  def pluginsAndFiles = SerialiserService.pluginsAndFiles(runnableTasks)

  def usedFiles: Iterable[File] = {
    val pf = pluginsAndFiles
    import pf._
    files ++
      Seq(environment.runtime, environment.jvmLinuxI386, environment.jvmLinuxX64) ++
      environment.plugins ++
      plugins
  }

  def usedFileHashes = usedFiles.map(f ⇒ (f, FileService.hash(job.moleExecution, f)))

  val environment: BatchEnvironment

  def selectStorage(): (StorageService, AccessToken)
  def selectJobService(): (JobService, AccessToken)
}

trait BatchEnvironment extends Environment with JobList { env ⇒
  type SS <: StorageService
  type JS <: JobService

  def jobs = batchJobWatcher.executionJobs

  def executionJob(job: Job): BatchExecutionJob

  def openMOLEMemory: Option[Int] = None
  def openMOLEMemoryValue = openMOLEMemory match {
    case None    ⇒ Workspace.preferenceAsInt(MemorySizeForRuntime)
    case Some(m) ⇒ m
  }

  @transient lazy val batchJobWatcher = {
    val watcher = new BatchJobWatcher(WeakReference(this))
    Updater.registerForUpdate(watcher)
    watcher
  }

  def threads: Option[Int] = None
  def threadsValue = threads.getOrElse(1)

  override def submit(job: Job) = {
    val bej = executionJob(job)
    EventDispatcher.trigger(this, new Environment.JobSubmitted(bej))
    batchJobWatcher.register(bej)
    jobManager ! Manage(bej)
  }

  def clean: Unit

  def runtime = BatchEnvironment.runtimeLocation
  def jvmLinuxI386 = BatchEnvironment.JVMLinuxI386Location
  def jvmLinuxX64 = BatchEnvironment.JVMLinuxX64Location

  @transient lazy val plugins = PluginManager.pluginsForClass(this.getClass)

  def minUpdateInterval = Workspace.preferenceAsDuration(MinUpdateInterval)
  def maxUpdateInterval = Workspace.preferenceAsDuration(MaxUpdateInterval)
  def incrementUpdateInterval = Workspace.preferenceAsDuration(IncrementUpdateInterval)

  def submitted: Long = jobs.count { _.state == ExecutionState.SUBMITTED }
  def running: Long = jobs.count { _.state == ExecutionState.RUNNING }

  def runtimeSettings = RuntimeSettings(archiveResult = false)
}

class SimpleBatchExecutionJob(val job: Job, val environment: SimpleBatchEnvironment) extends ExecutionJob with BatchExecutionJob { bej ⇒

  def selectStorage() = {
    val s = environment.storage
    (s, s.waitAToken)
  }
  def selectJobService() = {
    val js = environment.jobService
    (js, js.waitAToken)
  }

}

trait SimpleBatchEnvironment <: BatchEnvironment { env ⇒
  type BEJ = SimpleBatchExecutionJob

  def executionJob(job: Job): BEJ = new SimpleBatchExecutionJob(job, this)

  def storage: SS
  def jobService: JS

  def clean = ReplicaCatalog.withSession { session ⇒ storage.withToken(storage.clean(_, session)) }
}