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
import java.util.concurrent.{CountDownLatch, Semaphore}

import org.openmole.core.communication.message._
import org.openmole.core.communication.storage.{RemoteStorage, TransferOptions}
import org.openmole.core.event.{Event, EventDispatcher}
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.fileservice.{FileCache, FileService, FileServiceCache}
import org.openmole.core.location._
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.preference.{ConfigurationLocation, Preference}
import org.openmole.core.replication.ReplicaCatalog
import org.openmole.core.serializer.SerializerService
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.workflow.execution._
import org.openmole.core.workflow.job._
import org.openmole.core.workflow.mole.MoleServices
import org.openmole.core.workspace._
import org.openmole.plugin.environment.batch.environment.BatchEnvironment.ExecutionJobRegistry
import org.openmole.plugin.environment.batch.refresh._
import org.openmole.tool.cache._
import org.openmole.tool.file._
import org.openmole.tool.logger.JavaLogger
import org.openmole.tool.random.{RandomProvider, Seeder, shuffled}
import squants.information.Information
import squants.information.InformationConversions._
import squants.time.TimeConversions._
import org.openmole.tool.lock._

import scala.collection.immutable.TreeSet

object BatchEnvironment extends JavaLogger {

  trait Transfer {
    def id: Long
  }

  case class BeginUpload(id: Long, file: File, storageId: String) extends Event[BatchEnvironment] with Transfer
  case class EndUpload(id: Long, file: File, storageId: String, path: util.Try[String], size: Long) extends Event[BatchEnvironment] with Transfer {
    def success = path.isSuccess
  }

  case class BeginDownload(id: Long, file: File, path: String, storageId: String) extends Event[BatchEnvironment] with Transfer
  case class EndDownload(id: Long, file: File, path: String, storageId: String, exception: Option[Throwable]) extends Event[BatchEnvironment] with Transfer {
    def success = exception.isEmpty
    def size = file.size
  }

  def signalUpload(id: Long, upload: ⇒ String, file: File, environment: BatchEnvironment, storageId: String)(implicit eventDispatcher: EventDispatcher): String = {
    val size = file.size
    eventDispatcher.trigger(environment, BeginUpload(id, file, storageId))
    val path =
      try upload
      catch {
        case e: Throwable ⇒
          eventDispatcher.trigger(environment, EndUpload(id, file, storageId, util.Failure(e), size))
          throw e
      }

    eventDispatcher.trigger(environment, EndUpload(id, file, storageId, util.Success(path), size))
    path
  }

  def signalDownload[T](id: Long, download: ⇒ T, path: String, environment: BatchEnvironment, storageId: String, file: File)(implicit eventDispatcher: EventDispatcher): T = {
    eventDispatcher.trigger(environment, BeginDownload(id, file, path, storageId))
    val res =
      try download
      catch {
        case e: Throwable ⇒
          eventDispatcher.trigger(environment, EndDownload(id, file, path, storageId, Some(e)))
          throw e
      }
    eventDispatcher.trigger(environment, EndDownload(id, file, path, storageId, None))
    res
  }

  val MemorySizeForRuntime = ConfigurationLocation("BatchEnvironment", "MemorySizeForRuntime", Some(1024 megabytes))

  val CheckInterval = ConfigurationLocation("BatchEnvironment", "CheckInterval", Some(1 minutes))

  val GetTokenInterval = ConfigurationLocation("BatchEnvironment", "GetTokenInterval", Some(1 minutes))

  val MinUpdateInterval = ConfigurationLocation("BatchEnvironment", "MinUpdateInterval", Some(1 minutes))
  val MaxUpdateInterval = ConfigurationLocation("BatchEnvironment", "MaxUpdateInterval", Some(10 minutes))
  val IncrementUpdateInterval = ConfigurationLocation("BatchEnvironment", "IncrementUpdateInterval", Some(1 minutes))

  val MaxUpdateErrorsInARow = ConfigurationLocation("BatchEnvironment", "MaxUpdateErrorsInARow", Some(3))

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

  def threadsValue(threads: Option[Int]) = threads.getOrElse(1)

  object Services {

    implicit def fromServices(implicit services: org.openmole.core.services.Services): Services = {
      import services._
      new Services()
    }

    def copy(services: Services)(
      threadProvider:             ThreadProvider = services.threadProvider,
      preference:        Preference = services.preference,
      newFile:           NewFile = services.newFile,
      serializerService: SerializerService = services.serializerService,
      fileService:       FileService = services.fileService,
      seeder:            Seeder = services.seeder,
      randomProvider:    RandomProvider = services.randomProvider,
      replicaCatalog:    ReplicaCatalog = services.replicaCatalog,
      eventDispatcher:   EventDispatcher = services.eventDispatcher,
      fileServiceCache:  FileServiceCache = services.fileServiceCache) =
      new Services()(
        threadProvider = threadProvider,
        preference = preference,
        newFile = newFile,
        serializerService = serializerService,
        fileService = fileService,
        seeder = seeder,
        randomProvider = randomProvider,
        replicaCatalog = replicaCatalog,
        eventDispatcher = eventDispatcher,
        fileServiceCache = fileServiceCache)

    def set(services: Services)(ms: MoleServices) =
      new Services() (
        threadProvider = ms.threadProvider,
        preference = ms.preference,
        newFile = ms.newFile,
        serializerService = services.serializerService,
        fileService = ms.fileService,
        seeder = ms.seeder,
        randomProvider = services.randomProvider,
        replicaCatalog = services.replicaCatalog,
        eventDispatcher = ms.eventDispatcher,
        fileServiceCache = ms.fileServiceCache
      )

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
  ) { services =>

    def set(ms: MoleServices) = Services.set(services)(ms)

    def copy (
      threadProvider:    ThreadProvider = services.threadProvider,
      preference:        Preference = services.preference,
      newFile:           NewFile = services.newFile,
      serializerService: SerializerService = services.serializerService,
      fileService:       FileService = services.fileService,
      seeder:            Seeder = services.seeder,
      randomProvider:    RandomProvider = services.randomProvider,
      replicaCatalog:    ReplicaCatalog = services.replicaCatalog,
      eventDispatcher:   EventDispatcher = services.eventDispatcher,
      fileServiceCache:  FileServiceCache = services.fileServiceCache) =
      Services.copy(services)(
        threadProvider = threadProvider,
        preference = preference,
        newFile = newFile,
        serializerService = serializerService,
        fileService = fileService,
        seeder = seeder,
        randomProvider = randomProvider,
        replicaCatalog = replicaCatalog,
        eventDispatcher = eventDispatcher,
        fileServiceCache = fileServiceCache)
  }

  def jobFiles(job: BatchExecutionJob) =
    job.files.toVector ++
      job.plugins ++
      job.environment.plugins ++
      Seq(job.environment.jvmLinuxX64, job.environment.runtime)

  def toReplicatedFile(
    upload: (File, TransferOptions) => String,
    exist: String => Boolean,
    remove: String => Unit,
    environment: BatchEnvironment,
    storageId: String)(file: File, transferOptions: TransferOptions)(implicit services: BatchEnvironment.Services): ReplicatedFile = {
    import services._

    if (!file.exists) throw new UserBadDataError(s"File $file is required but doesn't exist.")

    val isDir = file.isDirectory
    val toReplicatePath = file.getCanonicalFile

    val (toReplicate, options) =
      if (isDir) (services.fileService.archiveForDir(file).file, transferOptions.copy(noLink = true))
      else (file, transferOptions)

    val fileMode = file.mode
    val hash = services.fileService.hash(toReplicate).toString

    def uploadReplica = signalUpload(eventDispatcher.eventId, upload(toReplicate, options), toReplicate, environment, storageId)

    val replica = services.replicaCatalog.uploadAndGet(uploadReplica, exist, remove, toReplicatePath, hash, storageId)
    ReplicatedFile(file.getPath, file.getName, isDir, hash, replica.path, fileMode)
  }
  
  def serializeJob(
    job: BatchExecutionJob,
    remoteStorage: RemoteStorage,
    replicate: (File, TransferOptions) => ReplicatedFile,
    upload: (File, TransferOptions) => String,
    storageId: String)(implicit services: BatchEnvironment.Services): SerializedJob = services.newFile.withTmpFile("job", ".tar") { jobFile ⇒

    import services._

    serializerService.serialize(job.runnableTasks, jobFile)

    val plugins = new TreeSet[File]()(fileOrdering) ++ job.plugins -- job.environment.plugins ++ (job.files.toSet & job.environment.plugins.toSet)
    val files = (new TreeSet[File]()(fileOrdering) ++ job.files) -- plugins

    val runtime = replicateTheRuntime(job.environment, replicate)

    val executionMessage = createExecutionMessage(
      jobFile,
      files,
      plugins,
      replicate,
      job.environment
    )

    /* ---- upload the execution message ----*/
    val inputPath =
      newFile.withTmpFile("job", ".tar") { executionMessageFile ⇒
        serializerService.serializeAndArchiveFiles(executionMessage, executionMessageFile)
        signalUpload(eventDispatcher.eventId, upload(executionMessageFile, TransferOptions(noLink = true, canMove = true)), executionMessageFile, job.environment, storageId)
      }

    val serializedStorage =
      services.newFile.withTmpFile("remoteStorage", ".tar") { storageFile ⇒
        import org.openmole.tool.hash._
        import services._
        services.serializerService.serializeAndArchiveFiles(remoteStorage, storageFile)
        val hash = storageFile.hash().toString()
        val path = signalUpload(eventDispatcher.eventId, upload(storageFile, TransferOptions(noLink = true, canMove = true, raw = true)), storageFile, job.environment, storageId)
        FileMessage(path, hash)
      }

    SerializedJob(inputPath, runtime, serializedStorage)
  }



  def replicateTheRuntime(
    environment:      BatchEnvironment,
    replicate: (File, TransferOptions) => ReplicatedFile,
  )(implicit services: BatchEnvironment.Services) = {
    val environmentPluginPath = shuffled(environment.plugins)(services.randomProvider()).map { p ⇒ replicate(p, TransferOptions(raw = true)) }.map { FileMessage(_) }
    val runtimeFileMessage = FileMessage(replicate(environment.runtime, TransferOptions(raw = true)))
    val jvmLinuxX64FileMessage = FileMessage(replicate(environment.jvmLinuxX64, TransferOptions(raw = true)))

    Runtime(
      runtimeFileMessage,
      environmentPluginPath.toSet,
      jvmLinuxX64FileMessage
    )
  }

  def createExecutionMessage(
    jobFile:             File,
    serializationFile:   Iterable[File],
    serializationPlugin: Iterable[File],
    replicate: (File, TransferOptions) => ReplicatedFile,
    environment: BatchEnvironment
  )(implicit services: BatchEnvironment.Services): ExecutionMessage = {

    val pluginReplicas = shuffled(serializationPlugin)(services.randomProvider()).map { replicate(_, TransferOptions(raw = true)) }
    val files = shuffled(serializationFile)(services.randomProvider()).map { replicate(_, TransferOptions()) }

    ExecutionMessage(
      pluginReplicas,
      files,
      jobFile,
      environment.runtimeSettings
    )
  }

  def isClean(environment: BatchEnvironment)(implicit services: BatchEnvironment.Services) = {
    val environmentJobs = environment.jobs
    environmentJobs.forall(_.state == ExecutionState.KILLED)
  }

  def finishedExecutionJob(environment: BatchEnvironment, job: BatchExecutionJob) = {
    ExecutionJobRegistry.finished(environment.registry, job, environment)
    environment.finishedJob(job)
  }

  object ExecutionJobRegistry {

    def register(registry: ExecutionJobRegistry, ejob: BatchExecutionJob) = registry.synchronized {
      registry.executionJobs = ejob :: registry.executionJobs
      registry.empty.drainPermits()
    }

    def finished(registry: ExecutionJobRegistry, job: BatchExecutionJob, environment: BatchEnvironment) = registry.synchronized {
      def pruneJobs(registry: ExecutionJobRegistry) = registry.executionJobs.filter(j => j != job)
      registry.executionJobs = pruneJobs(registry)
      if(registry.executionJobs.isEmpty) registry.empty.release(1)
    }

    def executionJobs(registry: ExecutionJobRegistry) = registry.synchronized { registry.executionJobs }
  }

  class ExecutionJobRegistry {
    var executionJobs = List[BatchExecutionJob]()
    val empty = new Semaphore(1)
  }

  def registryIsEmpty(environment: BatchEnvironment) = {
    environment.registry.empty.availablePermits() == 0
  }

  def waitJobKilled(environment: BatchEnvironment) = {
    environment.registry.empty.acquireAndRelease()
  }

  def defaultUpdateInterval(implicit preference: Preference) =
    UpdateInterval(
      minUpdateInterval = preference(BatchEnvironment.MinUpdateInterval),
      maxUpdateInterval = preference(BatchEnvironment.MaxUpdateInterval),
      incrementUpdateInterval = preference(BatchEnvironment.IncrementUpdateInterval)
    )
}

abstract class BatchEnvironment extends SubmissionEnvironment { env ⇒

  @volatile var stopped = false

  implicit val services: BatchEnvironment.Services
  def eventDispatcherService = services.eventDispatcher

  def exceptions = services.preference(Environment.maxExceptionsLog)
  def clean = BatchEnvironment.registryIsEmpty(env)

  lazy val registry = new ExecutionJobRegistry()

  def jobs = ExecutionJobRegistry.executionJobs(registry)

  lazy val relpClassesCache = new AssociativeCache[Set[String], (Seq[File], Seq[FileCache])]

  lazy val plugins = PluginManager.pluginsForClass(this.getClass)
  lazy val jobStore = JobStore(services.newFile.makeNewDir("jobstore"))


  override def submit(job: Job) = JobManager ! Manage(job, this)

  def execute(batchExecutionJob: BatchExecutionJob): BatchJobControl

  def runtime = BatchEnvironment.runtimeLocation
  def jvmLinuxX64 = BatchEnvironment.JVMLinuxX64Location

  def submitted: Long = jobs.count { _.state == ExecutionState.SUBMITTED }
  def running: Long = jobs.count { _.state == ExecutionState.RUNNING }

  def runtimeSettings = RuntimeSettings(archiveResult = false)

  def finishedJob(job: ExecutionJob) = {}

}

