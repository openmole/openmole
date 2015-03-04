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

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import java.io.File
import org.openmole.core.eventdispatcher.{ Event, EventDispatcher }
import java.util.concurrent.atomic.AtomicLong
import org.openmole.core.batch.control._
import org.openmole.core.batch.storage._
import org.openmole.core.batch.jobservice._
import org.openmole.core.batch.refresh._
import org.openmole.core.batch.replication._
import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.tools.io.FileUtil
import org.openmole.core.tools.service.{ Logger, Hash, ThreadUtil }
import FileUtil._
import org.openmole.core.updater.Updater
import org.openmole.core.workflow.job._
import org.openmole.core.workspace.{ Workspace, ConfigurationLocation }
import org.openmole.core.tools.service._
import org.openmole.core.workflow.execution._
import akka.actor.Props
import ThreadUtil._
import ref.WeakReference

object BatchEnvironment extends Logger {

  trait Transfer {
    def id: Long
  }

  val transferId = new AtomicLong

  case class BeginUpload(id: Long, path: String, storage: StorageService) extends Event[BatchEnvironment] with Transfer
  case class EndUpload(id: Long, path: String, storage: StorageService) extends Event[BatchEnvironment] with Transfer

  case class BeginDownload(id: Long, path: String, storage: StorageService) extends Event[BatchEnvironment] with Transfer
  case class EndDownload(id: Long, path: String, storage: StorageService) extends Event[BatchEnvironment] with Transfer

  def signalUpload[T](upload: ⇒ T, path: String, storage: StorageService) = {
    val id = transferId.getAndIncrement
    EventDispatcher.trigger(storage.environment, new BeginUpload(id, path, storage))
    try upload
    finally EventDispatcher.trigger(storage.environment, new EndUpload(id, path, storage))
  }

  def signalDownload[T](download: ⇒ T, path: String, storage: StorageService) = {
    val id = transferId.getAndIncrement
    EventDispatcher.trigger(storage.environment, new BeginDownload(id, path, storage))
    try download
    finally EventDispatcher.trigger(storage.environment, new EndDownload(id, path, storage))
  }

  val MemorySizeForRuntime = new ConfigurationLocation("BatchEnvironment", "MemorySizeForRuntime")

  val CheckInterval = new ConfigurationLocation("BatchEnvironment", "CheckInterval")

  val CheckFileExistsInterval = new ConfigurationLocation("BatchEnvironment", "CheckFileExistsInterval")

  val MinUpdateInterval = new ConfigurationLocation("BatchEnvironment", "MinUpdateInterval")
  val MaxUpdateInterval = new ConfigurationLocation("BatchEnvironment", "MaxUpdateInterval")
  val IncrementUpdateInterval = new ConfigurationLocation("BatchEnvironment", "IncrementUpdateInterval")
  val MaxUpdateErrorsInARow = ConfigurationLocation("BatchEnvironment", "MaxUpdateErrorsInARow")

  val JobManagementThreads = new ConfigurationLocation("BatchEnvironment", "JobManagementThreads")

  val EnvironmentCleaningThreads = new ConfigurationLocation("BatchEnvironment", "EnvironmentCleaningThreads")

  val StoragesGCUpdateInterval = new ConfigurationLocation("BatchEnvironment", "StoragesGCUpdateInterval")

  val NoTokenForServiceRetryInterval = new ConfigurationLocation("BatchEnvironment", "NoTokenForServiceRetryInterval")

  val MemoryMargin = ConfigurationLocation("BatchEnvironment", "MemoryMargin")

  Workspace += (MinUpdateInterval, "PT1M")
  Workspace += (MaxUpdateInterval, "PT10M")
  Workspace += (IncrementUpdateInterval, "PT1M")
  Workspace += (MaxUpdateErrorsInARow, "3")

  private def runtimeDirLocation = Workspace.openMOLELocation.getOrElse(throw new InternalProcessingError("openmole.location not set")).child("runtime")

  @transient lazy val runtimeLocation = runtimeDirLocation.child("runtime.tar.gz")
  @transient lazy val JVMLinuxI386Location = runtimeDirLocation.child("jvm-386.tar.gz")
  @transient lazy val JVMLinuxX64Location = runtimeDirLocation.child("jvm-x64.tar.gz")

  Workspace += (MemorySizeForRuntime, "1024")
  Workspace += (CheckInterval, "PT1M")
  Workspace += (CheckFileExistsInterval, "PT1H")
  Workspace += (JobManagementThreads, "200")
  Workspace += (EnvironmentCleaningThreads, "20")

  Workspace += (StoragesGCUpdateInterval, "PT1H")
  Workspace += (NoTokenForServiceRetryInterval, "PT2M")

  Workspace += (MemoryMargin, "1024")

  def defaultRuntimeMemory = Workspace.preferenceAsInt(BatchEnvironment.MemorySizeForRuntime)

  lazy val system = ActorSystem("BatchEnvironment", ConfigFactory.parseString(
    """
akka {
  daemonic="on"
  actor {
    default-dispatcher {
      executor = "fork-join-executor"
      type = Dispatcher

      fork-join-executor {
        parallelism-min = 1
        parallelism-max = 10
      }
    }
  }
}
    """).withFallback(ConfigFactory.load(classOf[ConfigFactory].getClassLoader)))

  lazy val jobManager = system.actorOf(Props(new JobManager))

}

import BatchEnvironment._

trait BatchEnvironment extends Environment { env ⇒

  //val id: String

  type SS <: StorageService
  type JS <: JobService

  def allStorages: Iterable[SS]
  def allJobServices: Iterable[JS]

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

  def clean = ReplicaCatalog.withSession { implicit c ⇒
    val cleaningThreadPool = fixedThreadPool(Workspace.preferenceAsInt(EnvironmentCleaningThreads))
    allStorages.foreach {
      s ⇒
        background {
          s.withToken { implicit t ⇒
            s.clean
          }
        }(cleaningThreadPool)
    }
  }

  def executionJob(job: Job) = new BatchExecutionJob(this, job)

  def runtime = BatchEnvironment.runtimeLocation
  def jvmLinuxI386 = BatchEnvironment.JVMLinuxI386Location
  def jvmLinuxX64 = BatchEnvironment.JVMLinuxX64Location

  @transient lazy val jobServices = {
    val jobServices = allJobServices
    if (jobServices.isEmpty) throw new InternalProcessingError("No job service available for the environment.")
    jobServices
  }

  @transient lazy val storages = {
    val storages = allStorages
    if (storages.isEmpty) throw new InternalProcessingError("No storage service available for the environment.")
    Updater.registerForUpdate(new StoragesGC(WeakReference(storages)), Workspace.preferenceAsDuration(StoragesGCUpdateInterval))
    storages
  }

  def selectAJobService: (JobService, AccessToken) = {
    val r = jobServices.head
    (r, r.waitAToken)
  }

  def selectAStorage(usedFileHashes: Iterable[(File, Hash)]): (StorageService, AccessToken) = {
    val r = storages.head
    (r, r.waitAToken)
  }

  @transient lazy val plugins = PluginManager.pluginsForClass(this.getClass)

  def minUpdateInterval = Workspace.preferenceAsDuration(MinUpdateInterval)
  def maxUpdateInterval = Workspace.preferenceAsDuration(MaxUpdateInterval)
  def incrementUpdateInterval = Workspace.preferenceAsDuration(IncrementUpdateInterval)

  def executionJobs: Iterable[BatchExecutionJob] = batchJobWatcher.executionJobs

}
