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
import java.util.UUID

import org.openmole.core.event.{ Event, EventDispatcher }
import java.util.concurrent.atomic.AtomicLong

import org.openmole.core.batch.control._
import org.openmole.core.batch.storage._
import org.openmole.core.batch.jobservice._
import org.openmole.core.batch.refresh._
import org.openmole.core.batch.replication._
import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.fileservice.{ FileCache, FileService }
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.serializer.SerialiserService
import org.openmole.tool.file._
import org.openmole.tool.logger.Logger
import org.openmole.tool.thread._
import org.openmole.core.updater.Updater
import org.openmole.core.workflow.job._
import org.openmole.core.workspace.{ ConfigurationLocation, Workspace }
import org.openmole.core.workflow.execution._
import org.openmole.core.batch.message._
import org.openmole.core.console.ScalaREPL.ReferencedClasses
import org.openmole.core.console.{ REPLClassloader, ScalaREPL }
import org.openmole.core.tools.cache.AssociativeCache
import org.openmole.tool.hash.Hash

import ref.WeakReference
import scala.Predef.Set
import scala.collection.mutable.{ HashMap, MultiMap, Set }
import concurrent.duration._

object BatchEnvironment extends Logger {

  trait Transfer {
    def id: Long
  }

  val transferId = new AtomicLong

  case class BeginUpload(id: Long, file: File, path: String, storage: StorageService) extends Event[BatchEnvironment] with Transfer
  case class EndUpload(id: Long, file: File, path: String, storage: StorageService, exception: Option[Throwable]) extends Event[BatchEnvironment] with Transfer {
    def success = !exception.isDefined
  }

  case class BeginDownload(id: Long, file: File, path: String, storage: StorageService) extends Event[BatchEnvironment] with Transfer
  case class EndDownload(id: Long, file: File, path: String, storage: StorageService, exception: Option[Throwable]) extends Event[BatchEnvironment] with Transfer {
    def success = !exception.isDefined
  }

  def signalUpload[T](upload: ⇒ T, file: File, path: String, storage: StorageService): T = {
    val id = transferId.getAndIncrement
    EventDispatcher.trigger(storage.environment, BeginUpload(id, file, path, storage))
    val res =
      try upload
      catch {
        case e: Throwable ⇒
          EventDispatcher.trigger(storage.environment, EndUpload(id, file, path, storage, Some(e)))
          throw e
      }
    EventDispatcher.trigger(storage.environment, EndUpload(id, file, path, storage, None))
    res
  }

  def signalDownload[T](download: ⇒ T, path: String, storage: StorageService, file: File): T = {
    val id = transferId.getAndIncrement
    EventDispatcher.trigger(storage.environment, BeginDownload(id, file, path, storage))
    val res =
      try download
      catch {
        case e: Throwable ⇒
          EventDispatcher.trigger(storage.environment, EndDownload(id, file, path, storage, Some(e)))
          throw e
      }
    EventDispatcher.trigger(storage.environment, EndDownload(id, file, path, storage, None))
    res
  }

  val MemorySizeForRuntime = ConfigurationLocation("BatchEnvironment", "MemorySizeForRuntime", Some(1024))

  val CheckInterval = ConfigurationLocation("BatchEnvironment", "CheckInterval", Some(1 minute))

  val CheckFileExistsInterval = ConfigurationLocation("BatchEnvironment", "CheckFileExistsInterval", Some(1 hour))

  val GetTokenInterval = ConfigurationLocation("BatchEnvironment", "GetTokenInterval", Some(1 minute))

  val MinUpdateInterval = ConfigurationLocation("BatchEnvironment", "MinUpdateInterval", Some(1 minute))
  val MaxUpdateInterval = ConfigurationLocation("BatchEnvironment", "MaxUpdateInterval", Some(10 minute))
  val IncrementUpdateInterval = ConfigurationLocation("BatchEnvironment", "IncrementUpdateInterval", Some(1 minute))
  val MaxUpdateErrorsInARow = ConfigurationLocation("BatchEnvironment", "MaxUpdateErrorsInARow", Some(3))

  val JobManagementThreads = ConfigurationLocation("BatchEnvironment", "JobManagementThreads", Some(100))

  val StoragesGCUpdateInterval = ConfigurationLocation("BatchEnvironment", "StoragesGCUpdateInterval", Some(1 hour))
  val RuntimeMemoryMargin = ConfigurationLocation("BatchEnvironment", "RuntimeMemoryMargin", Some(400))

  val downloadResultRetry = ConfigurationLocation("BatchEnvironment", "DownloadResultRetry", Some(3))

  Workspace setDefault MinUpdateInterval
  Workspace setDefault MaxUpdateInterval
  Workspace setDefault IncrementUpdateInterval
  Workspace setDefault MaxUpdateErrorsInARow
  Workspace setDefault GetTokenInterval

  private def runtimeDirLocation = Workspace.openMOLELocation / "runtime"

  @transient lazy val runtimeLocation = runtimeDirLocation / "runtime.tar.gz"
  @transient lazy val JVMLinuxX64Location = runtimeDirLocation / "jvm-x64.tar.gz"

  Workspace setDefault MemorySizeForRuntime
  Workspace setDefault CheckInterval
  Workspace setDefault CheckFileExistsInterval
  Workspace setDefault JobManagementThreads

  Workspace setDefault StoragesGCUpdateInterval

  Workspace setDefault RuntimeMemoryMargin

  def defaultRuntimeMemory = Workspace.preference(BatchEnvironment.MemorySizeForRuntime)
  def getTokenInterval = Workspace.preference(GetTokenInterval) * Workspace.rng.nextDouble

  lazy val jobManager = new JobManager
}

import BatchEnvironment._

object BatchExecutionJob {
  val replBundleCache = new AssociativeCache[Seq[Class[_]], FileCache]()
}

trait BatchExecutionJob extends ExecutionJob { bej ⇒
  def job: Job
  var serializedJob: Option[SerializedJob] = None
  var batchJob: Option[BatchJob] = None

  def moleJobs = job.moleJobs
  def runnableTasks = job.moleJobs.map(RunnableTask(_))

  def plugins = pluginsAndFiles.plugins ++ closureBundle.map(_.file) ++ referencedClosures.toSeq.flatMap(_.plugins)
  def files = pluginsAndFiles.files

  @transient private lazy val pluginsAndFiles = SerialiserService.pluginsAndFiles(runnableTasks)

  @transient private lazy val referencedClosures: Option[ReferencedClasses] = {
    if (pluginsAndFiles.replClasses.isEmpty) None
    else {
      def referenced =
        pluginsAndFiles.replClasses.map { c ⇒
          val replClassloader = c.getClassLoader.asInstanceOf[REPLClassloader]
          replClassloader.referencedClasses(c)
        }.fold(ReferencedClasses.empty)(ReferencedClasses.merge)
      Some(referenced)
    }
  }

  def closureBundle =
    pluginsAndFiles.replClasses.toList match {
      case Nil ⇒ None
      case classes ⇒
        val bundle = BatchExecutionJob.replBundleCache.cache(job.moleExecution, classes, preCompute = false) { rc ⇒
          val allClasses =
            classes.map { c ⇒
              val replClassloader = c.getClassLoader.asInstanceOf[REPLClassloader]
              replClassloader.referencedClasses(c)
            }.fold(ReferencedClasses.empty)(ReferencedClasses.merge)

          val bundle = Workspace.newFile("closureBundle", ".jar")

          try ScalaREPL.bundleFromReferencedClass(allClasses, "closure-" + UUID.randomUUID.toString, "1.0", bundle)
          catch {
            case e: Throwable ⇒
              e.printStackTrace()
              bundle.delete()
              throw e
          }
          FileCache(bundle)
        }
        Some(bundle)
    }

  def usedFiles: Iterable[File] =
    (files ++
      Seq(environment.runtime, environment.jvmLinuxX64) ++
      environment.plugins ++ plugins).distinct

  def usedFileHashes = usedFiles.map(f ⇒ (f, FileService.hash(job.moleExecution, f)))

  def environment: BatchEnvironment

  def trySelectStorage(): Option[(StorageService, AccessToken)]
  def trySelectJobService(): Option[(JobService, AccessToken)]
}

trait BatchEnvironment extends SubmissionEnvironment { env ⇒
  type SS <: StorageService
  type JS <: JobService

  def jobs = batchJobWatcher.executionJobs

  def executionJob(job: Job): BatchExecutionJob

  def openMOLEMemory: Option[Int] = None
  def openMOLEMemoryValue = openMOLEMemory match {
    case None    ⇒ Workspace.preference(MemorySizeForRuntime)
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

  def runtime = BatchEnvironment.runtimeLocation
  def jvmLinuxX64 = BatchEnvironment.JVMLinuxX64Location

  @transient lazy val plugins = PluginManager.pluginsForClass(this.getClass)

  def minUpdateInterval = Workspace.preference(MinUpdateInterval)
  def maxUpdateInterval = Workspace.preference(MaxUpdateInterval)
  def incrementUpdateInterval = Workspace.preference(IncrementUpdateInterval)

  def submitted: Long = jobs.count { _.state == ExecutionState.SUBMITTED }
  def running: Long = jobs.count { _.state == ExecutionState.RUNNING }

  def runtimeSettings = RuntimeSettings(archiveResult = false)
}

class SimpleBatchExecutionJob(val job: Job, val environment: SimpleBatchEnvironment) extends ExecutionJob with BatchExecutionJob { bej ⇒

  def trySelectStorage() = {
    val s = environment.storage
    s.tryGetToken.map(t ⇒ (s, t))
  }
  def trySelectJobService() = {
    val js = environment.jobService
    js.tryGetToken.map(t ⇒ (js, t))
  }

}

trait SimpleBatchEnvironment <: BatchEnvironment { env ⇒
  type BEJ = SimpleBatchExecutionJob

  def executionJob(job: Job): BEJ = new SimpleBatchExecutionJob(job, this)

  def storage: SS
  def jobService: JS
}