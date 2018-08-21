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

package org.openmole.plugin.environment.batch.environment

import java.io.File
import java.util.UUID

import org.openmole.core.communication.message._
import org.openmole.core.communication.storage.{ RemoteStorage, TransferOptions }
import org.openmole.core.console.ScalaREPL.ReferencedClasses
import org.openmole.core.console.{ REPLClassloader, ScalaREPL }
import org.openmole.core.event.{ Event, EventDispatcher }
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.fileservice.{ FileCache, FileService, FileServiceCache }
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.preference.{ ConfigurationLocation, Preference }
import org.openmole.core.replication.ReplicaCatalog
import org.openmole.core.serializer.SerializerService
import org.openmole.core.threadprovider.{ ThreadProvider, Updater }
import org.openmole.core.workflow.execution._
import org.openmole.core.workflow.job._
import org.openmole.core.workspace._
import org.openmole.plugin.environment.batch.jobservice._
import org.openmole.plugin.environment.batch.refresh._
import org.openmole.plugin.environment.batch.storage._
import org.openmole.tool.cache._
import org.openmole.tool.file._
import org.openmole.tool.logger.JavaLogger
import org.openmole.tool.random.{ RandomProvider, Seeder, shuffled }
import squants.time.TimeConversions._
import squants.information.Information
import squants.information.InformationConversions._
import org.openmole.core.location._
import org.openmole.plugin.environment.batch
import org.openmole.plugin.environment.batch.environment.BatchEnvironment.{ ExecutionJobRegistry, signalUpload }

import scala.collection.immutable.TreeSet
import scala.ref.WeakReference
import scala.util.Random

object BatchEnvironment extends JavaLogger {

  trait Transfer {
    def id: Long
  }

  case class BeginUpload(id: Long, file: File, path: String, storage: StorageService[_]) extends Event[BatchEnvironment] with Transfer
  case class EndUpload(id: Long, file: File, path: String, storage: StorageService[_], exception: Option[Throwable], size: Long) extends Event[BatchEnvironment] with Transfer {
    def success = exception.isEmpty
  }

  case class BeginDownload(id: Long, file: File, path: String, storage: StorageService[_]) extends Event[BatchEnvironment] with Transfer
  case class EndDownload(id: Long, file: File, path: String, storage: StorageService[_], exception: Option[Throwable]) extends Event[BatchEnvironment] with Transfer {
    def success = exception.isEmpty
    def size = file.size
  }

  def signalUpload[T](id: Long, upload: ⇒ T, file: File, path: String, storage: StorageService[_])(implicit eventDispatcher: EventDispatcher): T = {
    val size = file.size
    eventDispatcher.trigger(storage.environment, BeginUpload(id, file, path, storage))
    val res =
      try upload
      catch {
        case e: Throwable ⇒
          eventDispatcher.trigger(storage.environment, EndUpload(id, file, path, storage, Some(e), size))
          throw e
      }
    eventDispatcher.trigger(storage.environment, EndUpload(id, file, path, storage, None, size))
    res
  }

  def signalDownload[T](id: Long, download: ⇒ T, path: String, storage: StorageService[_], file: File)(implicit eventDispatcher: EventDispatcher): T = {
    eventDispatcher.trigger(storage.environment, BeginDownload(id, file, path, storage))
    val res =
      try download
      catch {
        case e: Throwable ⇒
          eventDispatcher.trigger(storage.environment, EndDownload(id, file, path, storage, Some(e)))
          throw e
      }
    eventDispatcher.trigger(storage.environment, EndDownload(id, file, path, storage, None))
    res
  }

  val MemorySizeForRuntime = ConfigurationLocation("BatchEnvironment", "MemorySizeForRuntime", Some(1024 megabytes))

  val CheckInterval = ConfigurationLocation("BatchEnvironment", "CheckInterval", Some(1 minutes))

  val GetTokenInterval = ConfigurationLocation("BatchEnvironment", "GetTokenInterval", Some(1 minutes))

  val MinUpdateInterval = ConfigurationLocation("BatchEnvironment", "MinUpdateInterval", Some(1 minutes))
  val MaxUpdateInterval = ConfigurationLocation("BatchEnvironment", "MaxUpdateInterval", Some(10 minutes))
  val IncrementUpdateInterval = ConfigurationLocation("BatchEnvironment", "IncrementUpdateInterval", Some(1 minutes))
  val MaxUpdateErrorsInARow = ConfigurationLocation("BatchEnvironment", "MaxUpdateErrorsInARow", Some(3))
  val RuntimeMemoryMargin = ConfigurationLocation("BatchEnvironment", "RuntimeMemoryMargin", Some(400 megabytes))

  val downloadResultRetry = ConfigurationLocation("BatchEnvironment", "DownloadResultRetry", Some(3))
  val killJobRetry = ConfigurationLocation("BatchEnvironment", "KillJobRetry", Some(3))
  val cleanJobRetry = ConfigurationLocation("BatchEnvironment", "KillJobRetry", Some(3))

  val QualityHysteresis = ConfigurationLocation("BatchEnvironment", "QualityHysteresis", Some(100))

  private def runtimeDirLocation = openMOLELocation / "runtime"

  lazy val runtimeLocation = runtimeDirLocation / "runtime.tar.gz"
  lazy val JVMLinuxX64Location = runtimeDirLocation / "jvm-x64.tar.gz"

  def defaultRuntimeMemory(implicit preference: Preference) = preference(BatchEnvironment.MemorySizeForRuntime)
  def getTokenInterval(implicit preference: Preference, randomProvider: RandomProvider) = preference(GetTokenInterval) * randomProvider().nextDouble

  def openMOLEMemoryValue(openMOLEMemory: Option[Information])(implicit preference: Preference) = openMOLEMemory match {
    case None    ⇒ preference(MemorySizeForRuntime)
    case Some(m) ⇒ m
  }

  def requiredMemory(openMOLEMemory: Option[Information], memory: Option[Information])(implicit preference: Preference) = memory match {
    case Some(m) ⇒ m
    case None    ⇒ openMOLEMemoryValue(openMOLEMemory) + preference(BatchEnvironment.RuntimeMemoryMargin)
  }

  def threadsValue(threads: Option[Int]) = threads.getOrElse(1)

  object Services {

    implicit def fromServices(implicit services: org.openmole.core.services.Services): Services = {
      import services._
      new Services()
    }
  }

  class Services(
    implicit
    val threadProvider:             ThreadProvider,
    implicit val preference:        Preference,
    implicit val newFile:           NewFile,
    implicit val serializerService: SerializerService,
    implicit val fileService:       FileService,
    implicit val seeder:            Seeder,
    implicit val randomProvider:    RandomProvider,
    implicit val replicaCatalog:    ReplicaCatalog,
    implicit val eventDispatcher:   EventDispatcher,
    implicit val fileServiceCache:  FileServiceCache
  )

  def serializeJob[S](storage: S, remoteStorage: RemoteStorage, job: BatchExecutionJob, communicationPath: Lazy[String], replicaDirectory: String, clean: String ⇒ Unit)(implicit services: BatchEnvironment.Services, storageInterface: StorageInterface[S], environmentStorage: EnvironmentStorage[S]) = {
    val storageService = StorageService(storage)
    AccessControl.tryWithPermit(storageService.accessControl) {
      initCommunication(job, storageService, remoteStorage, communicationPath, replicaDirectory, clean)
    }
  }

  def jobFiles(job: BatchExecutionJob) =
    job.pluginsAndFiles.files.toVector ++
      job.pluginsAndFiles.plugins ++
      job.environment.plugins ++
      Seq(job.environment.jvmLinuxX64, job.environment.runtime)

  def initCommunication(job: BatchExecutionJob, storage: StorageService[_], remoteStorage: RemoteStorage, communicationPath: Lazy[String], replicaDirectory: String, clean: String ⇒ Unit)(implicit services: BatchEnvironment.Services): SerializedJob = services.newFile.withTmpFile("job", ".tar") { jobFile ⇒
    import services._

    serializerService.serialise(job.runnableTasks, jobFile)

    val plugins = new TreeSet[File]()(fileOrdering) ++ job.plugins
    val files = (new TreeSet[File]()(fileOrdering) ++ job.files) diff plugins

    val inputPath = storage.child(communicationPath(), uniqName("job", ".in"))
    val outputPath = storage.child(communicationPath(), uniqName("job", ".out"))

    val runtime = replicateTheRuntime(job.job, job.environment, storage, replicaDirectory)

    val executionMessage = createExecutionMessage(
      job.job,
      jobFile,
      files,
      plugins,
      storage,
      communicationPath(),
      replicaDirectory
    )

    /* ---- upload the execution message ----*/
    newFile.withTmpFile("job", ".tar") { executionMessageFile ⇒
      serializerService.serialiseAndArchiveFiles(executionMessage, executionMessageFile)
      signalUpload(eventDispatcher.eventId, storage.upload(executionMessageFile, inputPath, TransferOptions(forceCopy = true, canMove = true)), executionMessageFile, inputPath, storage)
    }

    val serializedStorage =
      services.newFile.withTmpFile("remoteStorage", ".tar") { storageFile ⇒
        import services._
        import org.openmole.tool.hash._
        services.serializerService.serialiseAndArchiveFiles(remoteStorage, storageFile)
        val hash = storageFile.hash().toString()
        val path = storage.child(communicationPath(), StorageSpace.timedUniqName)
        signalUpload(eventDispatcher.eventId, storage.upload(storageFile, path, TransferOptions(forceCopy = true, canMove = true)), storageFile, inputPath, storage)
        FileMessage(path, hash)
      }

    SerializedJob(storage, communicationPath(), inputPath, runtime, serializedStorage, Some(outputPath), clean)
  }

  def toReplicatedFile(file: File, storage: StorageService[_], replicaDirectory: String, transferOptions: TransferOptions)(implicit services: BatchEnvironment.Services): ReplicatedFile = {
    import services._

    if (!file.exists) throw new UserBadDataError(s"File $file is required but doesn't exist.")

    val isDir = file.isDirectory
    val toReplicatePath = file.getCanonicalFile

    val (toReplicate, options) =
      if (isDir) (services.fileService.archiveForDir(file).file, transferOptions.copy(forceCopy = true))
      else (file, transferOptions)

    val fileMode = file.mode
    val hash = services.fileService.hash(toReplicate).toString

    def upload = {
      val name = StorageSpace.timedUniqName
      val newFile = storage.child(replicaDirectory, name)
      signalUpload(eventDispatcher.eventId, storage.upload(toReplicate, newFile, options), toReplicate, newFile, storage)
      newFile
    }

    val replica = services.replicaCatalog.uploadAndGet(upload, toReplicatePath, hash, storage)
    ReplicatedFile(file.getPath, file.getName, isDir, hash, replica.path, fileMode)
  }

  def replicateTheRuntime(
    job:              Job,
    environment:      BatchEnvironment,
    storage:          StorageService[_],
    replicaDirectory: String
  )(implicit services: BatchEnvironment.Services) = {
    val environmentPluginPath = shuffled(environment.plugins)(services.randomProvider()).map { p ⇒ toReplicatedFile(p, storage, replicaDirectory, TransferOptions(raw = true)) }.map { FileMessage(_) }
    val runtimeFileMessage = FileMessage(toReplicatedFile(environment.runtime, storage, replicaDirectory, TransferOptions(raw = true)))
    val jvmLinuxX64FileMessage = FileMessage(toReplicatedFile(environment.jvmLinuxX64, storage, replicaDirectory, TransferOptions(raw = true)))

    Runtime(
      runtimeFileMessage,
      environmentPluginPath.toSet,
      jvmLinuxX64FileMessage
    )
  }

  def createExecutionMessage(
    job:                 Job,
    jobFile:             File,
    serializationFile:   Iterable[File],
    serializationPlugin: Iterable[File],
    storage:             StorageService[_],
    path:                String,
    replicaDirectory:    String
  )(implicit services: BatchEnvironment.Services): ExecutionMessage = {

    val pluginReplicas = shuffled(serializationPlugin)(services.randomProvider()).map { toReplicatedFile(_, storage, replicaDirectory, TransferOptions(raw = true)) }
    val files = shuffled(serializationFile)(services.randomProvider()).map { toReplicatedFile(_, storage, replicaDirectory, TransferOptions()) }

    ExecutionMessage(
      pluginReplicas,
      files,
      jobFile,
      path,
      storage.environment.runtimeSettings
    )
  }

  def resultPathInSerializedJob(serializedJob: SerializedJob) = serializedJob.resultPath.get

  def submitSerializedJob[S](jobService: S, serializedJob: SerializedJob, resultPath: SerializedJob ⇒ String = resultPathInSerializedJob)(implicit jobServiceInterface: JobServiceInterface[S]) =
    AccessControl.tryWithPermit(jobServiceInterface.accessControl(jobService)) {
      BatchJobService.submit(jobService, serializedJob, () ⇒ resultPath(serializedJob))
    }

  def isClean(environment: BatchEnvironment)(implicit services: BatchEnvironment.Services) = {
    val environmentJobs = environment.jobs
    environmentJobs.forall(_.state == ExecutionState.KILLED)
  }

  def finishedJob(environment: BatchEnvironment, job: Job) = {
    ExecutionJobRegistry.finished(environment.registry, job, environment)
  }

  def finishedExecutionJob(environment: BatchEnvironment, job: BatchExecutionJob) = {
    ExecutionJobRegistry.finished(environment.registry, job, environment)
    environment.finishedJob(job)
  }

  def numberOfExecutionJobs(environment: BatchEnvironment, job: Job) = {
    ExecutionJobRegistry.numberOfExecutionJobs(environment.registry, job)
  }

  object ExecutionJobRegistry {
    def register(registry: ExecutionJobRegistry, ejob: BatchExecutionJob) = registry.synchronized {
      registry.executionJobs = ejob :: registry.executionJobs
    }

    def finished(registry: ExecutionJobRegistry, job: Job, environment: BatchEnvironment) = registry.synchronized {
      val (newExecutionJobs, removed) = registry.executionJobs.partition(_.job != job)
      registry.executionJobs = newExecutionJobs
      removed
    }

    def finished(registry: ExecutionJobRegistry, job: BatchExecutionJob, environment: BatchEnvironment) = registry.synchronized {
      def pruneFinishedJobs(registry: ExecutionJobRegistry) = registry.executionJobs = registry.executionJobs.filter(!_.state.isFinal)
      pruneFinishedJobs(registry)
    }

    def executionJobs(registry: ExecutionJobRegistry) = registry.synchronized { registry.executionJobs }
    def numberOfExecutionJobs(registry: ExecutionJobRegistry, job: Job) = registry.synchronized {
      registry.executionJobs.count(_.job == job)
    }
  }

  class ExecutionJobRegistry {
    var executionJobs = List[BatchExecutionJob]()
  }
}

abstract class BatchEnvironment extends SubmissionEnvironment { env ⇒

  implicit val services: BatchEnvironment.Services
  def eventDispatcherService = services.eventDispatcher

  def exceptions = services.preference(Environment.maxExceptionsLog)

  def serializeJob(batchExecutionJob: BatchExecutionJob): Option[SerializedJob]
  def submitSerializedJob(serializedJob: SerializedJob): Option[BatchJobControl]

  def clean = BatchEnvironment.isClean(this)

  lazy val registry = new ExecutionJobRegistry()
  def jobs = ExecutionJobRegistry.executionJobs(registry)

  lazy val replBundleCache = new AssociativeCache[ReferencedClasses, FileCache]()

  lazy val plugins = PluginManager.pluginsForClass(this.getClass)

  override def submit(job: Job) = {
    import services._
    val bej = BatchExecutionJob(job, this)
    ExecutionJobRegistry.register(registry, bej)
    eventDispatcherService.trigger(this, new Environment.JobSubmitted(bej))
    JobManager ! Manage(bej)
  }

  def runtime = BatchEnvironment.runtimeLocation
  def jvmLinuxX64 = BatchEnvironment.JVMLinuxX64Location

  def updateInterval =
    UpdateInterval(
      minUpdateInterval = services.preference(BatchEnvironment.MinUpdateInterval),
      maxUpdateInterval = services.preference(BatchEnvironment.MaxUpdateInterval),
      incrementUpdateInterval = services.preference(BatchEnvironment.IncrementUpdateInterval)
    )

  def submitted: Long = jobs.count { _.state == ExecutionState.SUBMITTED }
  def running: Long = jobs.count { _.state == ExecutionState.RUNNING }

  def runtimeSettings = RuntimeSettings(archiveResult = false)

  def finishedJob(job: ExecutionJob) = {}

}

object BatchExecutionJob {
  def apply(job: Job, environment: BatchEnvironment) = new BatchExecutionJob(job, environment)
}

class BatchExecutionJob(val job: Job, val environment: BatchEnvironment) extends ExecutionJob { bej ⇒

  def moleJobs = job.moleJobs
  def runnableTasks = job.moleJobs.map(RunnableTask(_))

  def plugins = pluginsAndFiles.plugins ++ closureBundle.map(_.file) ++ referencedClosures.toSeq.flatMap(_.plugins)
  def files = pluginsAndFiles.files

  @transient lazy val pluginsAndFiles = environment.services.serializerService.pluginsAndFiles(runnableTasks)

  @transient lazy val referencedClosures = {
    if (pluginsAndFiles.replClasses.isEmpty) None
    else {
      def referenced =
        pluginsAndFiles.replClasses.map { c ⇒
          val replClassloader = c.getClassLoader.asInstanceOf[REPLClassloader]
          replClassloader.referencedClasses(Seq(c))
        }.fold(ReferencedClasses.empty)(ReferencedClasses.merge)
      Some(referenced)
    }
  }

  def closureBundle =
    referencedClosures.map { closures ⇒
      environment.replBundleCache.cache(job.moleExecution, closures, preCompute = false) { rc ⇒
        val bundle = environment.services.newFile.newFile("closureBundle", ".jar")
        try ScalaREPL.bundleFromReferencedClass(closures, "closure-" + UUID.randomUUID.toString, "1.0", bundle)
        catch {
          case e: Throwable ⇒
            bundle.delete()
            throw e
        }
        FileCache(bundle)(environment.services.fileService)
      }
    }

  def usedFiles: Iterable[File] =
    (files ++
      Seq(environment.runtime, environment.jvmLinuxX64) ++
      environment.plugins ++ plugins).distinct

  def usedFileHashes = usedFiles.map(f ⇒ (f, environment.services.fileService.hash(f)(environment.services.newFile, environment.services.fileServiceCache)))

}