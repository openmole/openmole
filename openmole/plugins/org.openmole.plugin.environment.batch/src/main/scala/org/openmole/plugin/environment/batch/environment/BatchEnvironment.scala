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
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{CountDownLatch, Semaphore}

import org.openmole.core.communication.message._
import org.openmole.core.communication.storage.{RemoteStorage, TransferOptions}
import org.openmole.core.event.{Event, EventDispatcher}
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.fileservice.{FileService, FileServiceCache}
import org.openmole.core.location._
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.preference.{PreferenceLocation, Preference}
import org.openmole.core.replication.ReplicaCatalog
import org.openmole.core.serializer.SerializerService
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.workflow.execution._
import org.openmole.core.workflow.job._
import org.openmole.core.workflow.mole.MoleServices
import org.openmole.core.workflow.tools.ExceptionEvent
import org.openmole.core.workspace._
import org.openmole.plugin.environment.batch.environment.BatchEnvironment.ExecutionJobRegistry
import org.openmole.plugin.environment.batch.refresh._
import org.openmole.tool.cache._
import org.openmole.tool.collection.RingBuffer
import org.openmole.tool.file._
import org.openmole.tool.logger.{JavaLogger, LoggerService}
import org.openmole.tool.random.{RandomProvider, Seeder, shuffled}
import squants.information.Information
import squants.information.InformationConversions._
import squants.time.TimeConversions._
import org.openmole.tool.lock._
import org.openmole.tool.outputredirection.OutputRedirection
import org.openmole.core.compiler.CompilationContext


import scala.collection.immutable.TreeSet

object BatchEnvironment {

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

  val MemorySizeForRuntime = PreferenceLocation("BatchEnvironment", "MemorySizeForRuntime", Some(1024 megabytes))
  
  val CheckInterval = PreferenceLocation("BatchEnvironment", "CheckInterval", Some(1 minutes))
  val SubmitRetryInterval = PreferenceLocation("BatchEnvironment", "SubmitRetryInterval", Some(30 seconds))

  val GetTokenInterval = PreferenceLocation("BatchEnvironment", "GetTokenInterval", Some(1 minutes))

  val MinUpdateInterval = PreferenceLocation("BatchEnvironment", "MinUpdateInterval", Some(20 seconds))
  val IncrementUpdateInterval = PreferenceLocation("BatchEnvironment", "IncrementUpdateInterval", Some(20 seconds))
  val MaxUpdateInterval = PreferenceLocation("BatchEnvironment", "MaxUpdateInterval", Some(5 minutes))

  val MaxUpdateErrorsInARow = PreferenceLocation("BatchEnvironment", "MaxUpdateErrorsInARow", Some(3))

  val downloadResultRetry = PreferenceLocation("BatchEnvironment", "DownloadResultRetry", Some(3))
  val killJobRetry = PreferenceLocation("BatchEnvironment", "KillJobRetry", Some(3))
  val cleanJobRetry = PreferenceLocation("BatchEnvironment", "KillJobRetry", Some(3))

  val QualityHysteresis = PreferenceLocation("BatchEnvironment", "QualityHysteresis", Some(100))

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

    def apply(ms: MoleServices)(implicit replicaCatalog: ReplicaCatalog) =
      new Services(ms.compilationContext) (
        threadProvider = ms.threadProvider,
        preference = ms.preference,
        newFile = ms.tmpDirectory,
        serializerService = ms.serializerService,
        fileService = ms.fileService,
        seeder = ms.seeder,
        randomProvider = ms.newRandom,
        replicaCatalog = replicaCatalog,
        eventDispatcher = ms.eventDispatcher,
        fileServiceCache = ms.fileServiceCache,
        outputRedirection = ms.outputRedirection,
        loggerService = ms.loggerService
      )

    def copy(services: Services)(
      threadProvider:             ThreadProvider = services.threadProvider,
      preference:        Preference = services.preference,
      newFile:           TmpDirectory = services.newFile,
      serializerService: SerializerService = services.serializerService,
      fileService:       FileService = services.fileService,
      seeder:            Seeder = services.seeder,
      randomProvider:    RandomProvider = services.randomProvider,
      replicaCatalog:    ReplicaCatalog = services.replicaCatalog,
      eventDispatcher:   EventDispatcher = services.eventDispatcher,
      fileServiceCache:  FileServiceCache = services.fileServiceCache,
      outputRedirection: OutputRedirection = services.outputRedirection,
      loggerService: LoggerService) =
      new Services(services.compilationContext)(
        threadProvider = threadProvider,
        preference = preference,
        newFile = newFile,
        serializerService = serializerService,
        fileService = fileService,
        seeder = seeder,
        randomProvider = randomProvider,
        replicaCatalog = replicaCatalog,
        eventDispatcher = eventDispatcher,
        fileServiceCache = fileServiceCache,
        outputRedirection = outputRedirection,
        loggerService = loggerService)

//    def set(services: Services)(ms: MoleServices) =
//      new Services(ms.compilationContext) (
//        threadProvider = ms.threadProvider,
//        preference = ms.preference,
//        newFile = ms.tmpDirectory,
//        serializerService = services.serializerService,
//        fileService = ms.fileService,
//        seeder = ms.seeder,
//        randomProvider = services.randomProvider,
//        replicaCatalog = services.replicaCatalog,
//        eventDispatcher = ms.eventDispatcher,
//        fileServiceCache = ms.fileServiceCache,
//        outputRedirection = ms.outputRedirection,
//        loggerService = ms.loggerService
//      )

  }

  class Services(val compilationContext: Option[CompilationContext])(
    implicit
    val threadProvider:             ThreadProvider,
    implicit val preference:        Preference,
    implicit val newFile:           TmpDirectory,
    implicit val serializerService: SerializerService,
    implicit val fileService:       FileService,
    implicit val seeder:            Seeder,
    implicit val randomProvider:    RandomProvider,
    implicit val replicaCatalog:    ReplicaCatalog,
    implicit val eventDispatcher:   EventDispatcher,
    implicit val fileServiceCache:  FileServiceCache,
    implicit val outputRedirection: OutputRedirection,
    implicit val loggerService: LoggerService,
  ) { services =>

//    def set(ms: MoleServices) = Services.set(services)(ms)

    def copy (
               threadProvider:    ThreadProvider = services.threadProvider,
               preference:        Preference = services.preference,
               newFile:           TmpDirectory = services.newFile,
               serializerService: SerializerService = services.serializerService,
               fileService:       FileService = services.fileService,
               seeder:            Seeder = services.seeder,
               randomProvider:    RandomProvider = services.randomProvider,
               replicaCatalog:    ReplicaCatalog = services.replicaCatalog,
               eventDispatcher:   EventDispatcher = services.eventDispatcher,
               fileServiceCache:  FileServiceCache = services.fileServiceCache,
               outputRedirection: OutputRedirection = services.outputRedirection,
               loggerService: LoggerService = services.loggerService) =
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
        fileServiceCache = fileServiceCache,
        outputRedirection = outputRedirection,
        loggerService = loggerService)
  }

  def jobFiles(job: BatchExecutionJob, environment: BatchEnvironment) =
    job.files.toVector ++
      job.plugins ++
      environment.plugins ++
      Seq(environment.jvmLinuxX64, environment.runtime)

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
      if (isDir) (services.fileService.archiveForDir(file), transferOptions.copy(noLink = true))
      else (file, transferOptions)

    val fileMode = file.mode
    val hash = services.fileService.hash(toReplicate).toString

    def uploadReplica = signalUpload(eventDispatcher.eventId, upload(toReplicate, options), toReplicate, environment, storageId)

    val replica = services.replicaCatalog.uploadAndGet(uploadReplica, exist, remove, toReplicatePath, hash, storageId)
    ReplicatedFile(file.getPath, file.getName, isDir, hash, replica.path, fileMode)
  }
  
  def serializeJob(
    environment: BatchEnvironment,
    job: BatchExecutionJob,
    remoteStorage: RemoteStorage,
    replicate: (File, TransferOptions) => ReplicatedFile,
    upload: (File, TransferOptions) => String,
    storageId: String)(implicit services: BatchEnvironment.Services): SerializedJob = services.newFile.withTmpFile("job", ".tar") { jobFile ⇒

    import services._

    serializerService.serialize(job.runnableTasks, jobFile)

    val plugins = new TreeSet[File]()(fileOrdering) ++ job.plugins ++ (job.files.toSet & environment.plugins.toSet) // Exclude env plugins maybe
    val files = (new TreeSet[File]()(fileOrdering) ++ job.files) -- plugins

    val runtime = replicateTheRuntime(environment, replicate)

    val executionMessage = createExecutionMessage(
      jobFile,
      files,
      plugins,
      replicate,
      environment
    )

    /* ---- upload the execution message ----*/
    val inputPath =
      newFile.withTmpFile("job", ".tar") { executionMessageFile ⇒
        serializerService.serializeAndArchiveFiles(executionMessage, executionMessageFile)
        signalUpload(eventDispatcher.eventId, upload(executionMessageFile, TransferOptions(noLink = true, canMove = true)), executionMessageFile, environment, storageId)
      }

    val serializedStorage =
      services.newFile.withTmpFile("remoteStorage", ".tar") { storageFile ⇒
        import org.openmole.tool.hash._
        import services._
        services.serializerService.serializeAndArchiveFiles(remoteStorage, storageFile)
        val hash = storageFile.hash().toString()
        val path = signalUpload(eventDispatcher.eventId, upload(storageFile, TransferOptions(noLink = true, canMove = true, raw = true)), storageFile, environment, storageId)
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
      pluginReplicas.sortBy(_.originalPath),
      files.sortBy(_.originalPath),
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

  def setExecutionJobSate(environment: BatchEnvironment, job: BatchExecutionJob, newState: ExecutionState)(implicit eventDispatcher: EventDispatcher) = job.synchronized {
    import ExecutionState._

    if (job.state != KILLED && newState != job.state) {
      newState match {
        case DONE ⇒ environment._done.incrementAndGet()
        case FAILED ⇒
          if (job.state == DONE) environment._done.decrementAndGet()
          environment._failed.incrementAndGet()
        case _ ⇒
      }

      eventDispatcher.trigger(environment, Environment.JobStateChanged(job.id, job, newState, job.state))
      job._state = newState
    }
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

  type REPLClassCache = AssociativeCache[Set[String], Seq[File]]
}

abstract class BatchEnvironment extends SubmissionEnvironment { env ⇒

  @volatile var stopped = false

  implicit val services: BatchEnvironment.Services
  def eventDispatcherService = services.eventDispatcher

  private lazy val _errors = new RingBuffer[ExceptionEvent](services.preference(Environment.maxExceptionsLog))
  def error(e: ExceptionEvent) = _errors.put(e)

  def errors: Seq[ExceptionEvent] = _errors.elements
  def clearErrors: Seq[ExceptionEvent] = _errors.clear()

  def clean = BatchEnvironment.registryIsEmpty(env)

  val registry = new ExecutionJobRegistry()

  def jobs = ExecutionJobRegistry.executionJobs(registry)

  lazy val plugins = {
    def closureBundleAndPlugins = services.compilationContext.toSeq.flatMap { c =>
      import services._
      val (cb, file) = BatchExecutionJob.replClassesToPlugins(c.classDirectory, c.classLoader)
      cb.plugins ++ Seq(file)
    }

    (PluginManager.pluginsForClass(this.getClass).toSeq ++ closureBundleAndPlugins).distinctBy(_.getCanonicalPath)
  }

  lazy val jobStore = JobStore(services.newFile.makeNewDir("jobstore"))

  override def submit(job: JobGroup) = {
    val id = jobId.getAndIncrement()
    JobManager ! Manage(id, job, this)
    id
  }

  def execute(batchExecutionJob: BatchExecutionJob): BatchJobControl

  def runtime = BatchEnvironment.runtimeLocation
  def jvmLinuxX64 = BatchEnvironment.JVMLinuxX64Location

  def submitted: Long = jobs.count { _.state == ExecutionState.SUBMITTED }
  def running: Long = jobs.count { _.state == ExecutionState.RUNNING }
  def runningJobs = jobs.filter(_.state == ExecutionState.RUNNING)

  private val jobId = new AtomicLong(0L)
  private[environment] val _done = new AtomicLong(0L)
  private[environment] val _failed = new AtomicLong(0L)

  def done: Long = _done.get()
  def failed: Long = _failed.get()

  def runtimeSettings = RuntimeSettings(archiveResult = false)

  def finishedJob(job: ExecutionJob) = {}

}

