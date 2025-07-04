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
import org.openmole.core.communication.message.*
import org.openmole.core.communication.storage.{RemoteStorage, TransferOptions}
import org.openmole.core.event.{Event, EventDispatcher}
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.fileservice.{FileService, FileServiceCache}
import org.openmole.core.location.*
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.preference.{Preference, PreferenceLocation}
import org.openmole.core.replication.ReplicaCatalog
import org.openmole.core.serializer.SerializerService
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.workflow.execution.*
import org.openmole.core.workflow.job.*
import org.openmole.core.workflow.mole.MoleServices
import org.openmole.core.workspace.*
import org.openmole.plugin.environment.batch.environment.BatchEnvironment.ExecutionJobRegistry
import org.openmole.plugin.environment.batch.refresh.*
import org.openmole.tool.cache.*
import org.openmole.tool.collection.RingBuffer
import org.openmole.tool.file.*
import org.openmole.tool.logger.{JavaLogger, LoggerService}
import org.openmole.tool.random.{RandomProvider, Seeder, shuffled}
import squants.information.Information
import squants.information.InformationConversions.*
import squants.time.TimeConversions.*
import org.openmole.tool.lock.*
import org.openmole.tool.outputredirection.OutputRedirection
import org.openmole.core.compiler.CompilationContext
import org.openmole.tool.crypto.Cypher

import scala.collection.immutable.{LongMap, TreeSet}

object BatchEnvironment:

  trait Transfer:
    def id: Long

  case class BeginUpload(id: Long, file: File, storageId: String) extends Event[BatchEnvironment] with Transfer
  case class EndUpload(id: Long, file: File, storageId: String, path: util.Try[String], size: Long) extends Event[BatchEnvironment] with Transfer {
    def success = path.isSuccess
  }

  case class BeginDownload(id: Long, file: File, path: String, storageId: String) extends Event[BatchEnvironment] with Transfer
  case class EndDownload(id: Long, file: File, path: String, storageId: String, exception: Option[Throwable]) extends Event[BatchEnvironment] with Transfer {
    def success = exception.isEmpty
    def size = file.size
  }

  def signalUpload(id: Long, upload: => String, file: File, environment: BatchEnvironment, storageId: String)(implicit eventDispatcher: EventDispatcher): String =
    val size = file.size
    eventDispatcher.trigger(environment, BeginUpload(id, file, storageId))
    val path =
      try upload
      catch {
        case e: Throwable =>
          eventDispatcher.trigger(environment, EndUpload(id, file, storageId, util.Failure(e), size))
          throw e
      }

    eventDispatcher.trigger(environment, EndUpload(id, file, storageId, util.Success(path), size))
    path

  def signalDownload[T](id: Long, download: => T, path: String, environment: BatchEnvironment, storageId: String, file: File)(implicit eventDispatcher: EventDispatcher): T =
    eventDispatcher.trigger(environment, BeginDownload(id, file, path, storageId))
    val res =
      try download
      catch {
        case e: Throwable =>
          eventDispatcher.trigger(environment, EndDownload(id, file, path, storageId, Some(e)))
          throw e
      }
    eventDispatcher.trigger(environment, EndDownload(id, file, path, storageId, None))
    res

  val MemorySizeForRuntime = PreferenceLocation("BatchEnvironment", "MemorySizeForRuntime", Some(1024 megabytes))

  val SubmitRetryInterval = PreferenceLocation("BatchEnvironment", "SubmitRetryInterval", Some(30 seconds))

  val GetTokenInterval = PreferenceLocation("BatchEnvironment", "GetTokenInterval", Some(1 minutes))

  val MinUpdateInterval = PreferenceLocation("BatchEnvironment", "MinUpdateInterval", Some(20 seconds))
  val IncrementUpdateInterval = PreferenceLocation("BatchEnvironment", "IncrementUpdateInterval", Some(10 seconds))
  val MaxUpdateInterval = PreferenceLocation("BatchEnvironment", "MaxUpdateInterval", Some(5 minutes))
  val MaxUpdateErrorsInARow = PreferenceLocation("BatchEnvironment", "MaxUpdateErrorsInARow", Some(3))

  val downloadResultRetry = PreferenceLocation("BatchEnvironment", "DownloadResultRetry", Some(3))
  val killJobRetry = PreferenceLocation("BatchEnvironment", "KillJobRetry", Some(3))
  val cleanJobRetry = PreferenceLocation("BatchEnvironment", "KillJobRetry", Some(3))

  val checkJobCanceledInterval = PreferenceLocation("BatchEnvironment", "CheckJobCanceledInterval", Some(5 seconds))

  val QualityHysteresis = PreferenceLocation("BatchEnvironment", "QualityHysteresis", Some(100))

  private def runtimeDirLocation = openMOLELocation / "runtime"

  lazy val runtimeLocation = runtimeDirLocation / "runtime.tar.gz"
  lazy val JVMLinuxX64Location = runtimeDirLocation / "jvm-x64.tar.gz"

  def defaultRuntimeMemory(implicit preference: Preference) = preference(BatchEnvironment.MemorySizeForRuntime)
  def getTokenInterval(implicit preference: Preference, randomProvider: RandomProvider) = preference(GetTokenInterval) * randomProvider().nextDouble

  def openMOLEMemoryValue(openMOLEMemory: Option[Information])(implicit preference: Preference) = openMOLEMemory match 
    case None    => preference(MemorySizeForRuntime)
    case Some(m) => m

  object Services:

    def apply(ms: MoleServices)(implicit replicaCatalog: ReplicaCatalog) =
      new Services(ms.compilationContext) (using
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

  
  class Services(val compilationContext: Option[CompilationContext])(
    implicit
    val threadProvider:    ThreadProvider,
    val preference:        Preference,
    val newFile:           TmpDirectory,
    val serializerService: SerializerService,
    val fileService:       FileService,
    val seeder:            Seeder,
    val randomProvider:    RandomProvider,
    val replicaCatalog:    ReplicaCatalog,
    val eventDispatcher:   EventDispatcher,
    val fileServiceCache:  FileServiceCache,
    val outputRedirection: OutputRedirection,
    val loggerService:     LoggerService
  )

  def jobFiles(job: BatchExecutionJob, environment: BatchEnvironment) =
    job.files.toVector ++
      job.plugins ++
      environment.environmentPlugins ++
      environment.scriptPlugins ++
      Seq(environment.jvmLinuxX64, environment.runtime)

  def toReplicatedFile(
    upload: (File, TransferOptions) => String,
    exist: String => Boolean,
    remove: String => Unit,
    environment: BatchEnvironment,
    storageId: String)(file: File, transferOptions: TransferOptions)(using services: BatchEnvironment.Services): ReplicatedFile =
    import services._

    if !file.exists then throw new UserBadDataError(s"File $file is required but doesn't exist.")

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

  def serializeJob(
    environment: BatchEnvironment,
    runtimeSetting: Option[RuntimeSetting],
    job: BatchExecutionJob,
    remoteStorage: RemoteStorage,
    replicate: (File, TransferOptions) => ReplicatedFile,
    upload: (File, TransferOptions) => String,
    storageId: String,
    archiveResult: Boolean = false)(implicit services: BatchEnvironment.Services): SerializedJob =
    import services.*
    TmpDirectory.withTmpFile("job", ".tar"): jobFile =>
      def tasks: RunnableTaskSequence = job.runnableTasks
      serializerService.serialize(tasks, jobFile, gz = true)

      val plugins =
        new TreeSet[File]()(using fileOrdering) ++
          job.plugins ++
          environment.scriptPlugins --
          environment.environmentPlugins ++
          (job.files.toSet & environment.environmentPlugins.toSet)

      val files = (new TreeSet[File]()(using fileOrdering) ++ job.files) -- plugins

      val runtime = replicateTheRuntime(environment, replicate)

      val executionMessage =
        createExecutionMessage(
          jobFile,
          files,
          plugins,
          replicate,
          environment,
          runtimeSetting,
          archiveResult = archiveResult
        )

      /* ---- upload the execution message ----*/
      val inputPath =
        newFile.withTmpFile("job", ".tar"): executionMessageFile =>
          serializerService.serializeAndArchiveFiles(executionMessage, executionMessageFile, gz = true)
          signalUpload(eventDispatcher.eventId, upload(executionMessageFile, TransferOptions(noLink = true, canMove = true)), executionMessageFile, environment, storageId)


      val serializedStorage =
        services.newFile.withTmpFile("remoteStorage", ".tar"): storageFile =>
          import org.openmole.tool.hash._
          import services._
          services.serializerService.serializeAndArchiveFiles(remoteStorage, storageFile, gz = true)
          val hash = Hash.file(storageFile).toString()
          val path = signalUpload(eventDispatcher.eventId, upload(storageFile, TransferOptions(noLink = true, canMove = true, raw = true)), storageFile, environment, storageId)
          FileMessage(path, hash)
      
      SerializedJob(inputPath, runtime, serializedStorage, executionMessage)

  
  def replicateTheRuntime(
    environment:      BatchEnvironment,
    replicate: (File, TransferOptions) => ReplicatedFile,
  )(implicit services: BatchEnvironment.Services) =
    val environmentPluginPath = shuffled(environment.environmentPlugins)(using services.randomProvider()).map { p => replicate(p, TransferOptions(raw = true)) }.map { FileMessage(_) }.sortBy(_.path)
    val runtimeFileMessage = FileMessage(replicate(environment.runtime, TransferOptions(raw = true)))
    val jvmLinuxX64FileMessage = FileMessage(replicate(environment.jvmLinuxX64, TransferOptions(raw = true)))

    Runtime(
      runtimeFileMessage,
      environmentPluginPath,
      jvmLinuxX64FileMessage
    )

  def createExecutionMessage(
    jobFile:             File,
    serializationFile:   Iterable[File],
    serializationPlugin: Iterable[File],
    replicate: (File, TransferOptions) => ReplicatedFile,
    environment: BatchEnvironment,
    runtimeSetting: Option[RuntimeSetting],
    archiveResult: Boolean
  )(implicit services: BatchEnvironment.Services): ExecutionMessage =

    val pluginReplicas = shuffled(serializationPlugin)(using services.randomProvider()).map { replicate(_, TransferOptions(raw = true)) }
    val files = shuffled(serializationFile)(using services.randomProvider()).map { replicate(_, TransferOptions()) }

    ExecutionMessage(
      pluginReplicas.sortBy(_.originalPath),
      files.sortBy(_.originalPath),
      jobFile,
      runtimeSetting.getOrElse(RuntimeSetting()),
      archiveResult = archiveResult
    )

  def isClean(environment: BatchEnvironment)(using services: BatchEnvironment.Services) =
    val environmentJobs = environment.jobs
    environmentJobs.forall(j => BatchEnvironment.executionSate(environment, j) == ExecutionState.KILLED)

  def finishedExecutionJob(environment: BatchEnvironment, job: BatchExecutionJob) =
    ExecutionJobRegistry.finished(environment.registry, job, environment)
    BatchEnvironmentState.clearState(environment.state, job.id)
    environment.finishedJob(job)


  def executionSate(environment: BatchEnvironment, job: BatchExecutionJob): ExecutionState =
    BatchEnvironmentState.getState(environment.state, job.id)

  def setExecutionSate(environment: BatchEnvironment, job: BatchExecutionJob, newState: ExecutionState)(using eventDispatcher: EventDispatcher) = environment.state.synchronized:
    import ExecutionState._

    val state = BatchEnvironment.executionSate(environment, job)

    if state != KILLED && newState != state
    then
      newState match
        case DONE if state != DONE => environment.state._done.incrementAndGet()
        case FAILED =>
          if state == DONE then environment.state._done.decrementAndGet()
          environment.state._failed.incrementAndGet()
        case _ =>

      eventDispatcher.trigger(environment, Environment.JobStateChanged(job.id, job, newState, state))
      BatchEnvironmentState.putState(environment.state, job.id, newState)

  def submit(env: BatchEnvironment, job: JobGroup)(implicit services: BatchEnvironment.Services) =
    import services.*
    val id = env.state.jobId.getAndIncrement()
    val bej = BatchExecutionJob(id, job, env.state.jobStore)
    BatchEnvironmentState.putState(env.state, id, ExecutionState.READY)
    ExecutionJobRegistry.register(env.registry, bej)
    JobManager ! Manage(bej, env)
    id

  def environmentPlugins(env: BatchEnvironment) = PluginManager.pluginsForClass(env.getClass).toSeq.distinctBy(_.getCanonicalPath)

  def scriptPlugins(services: Services) =
    def closureBundleAndPlugins = 
      services.compilationContext.toSeq.flatMap: c =>
        import services._
        val (cb, file) = BatchExecutionJob.replClassesToPlugins(c.classDirectory, c.classLoader)
        cb.plugins ++ file.toSeq

    closureBundleAndPlugins.distinctBy(_.getCanonicalPath)


  object ExecutionJobRegistry:
    def register(registry: ExecutionJobRegistry, ejob: BatchExecutionJob) = registry.synchronized:
      registry.executionJobs = ejob :: registry.executionJobs
      registry.empty.drainPermits()

    def finished(registry: ExecutionJobRegistry, job: BatchExecutionJob, environment: BatchEnvironment) = registry.synchronized:
      def pruneJobs(registry: ExecutionJobRegistry, job: BatchExecutionJob) = registry.executionJobs.filter(j => j.id != job.id)
      registry.executionJobs = pruneJobs(registry, job)
      if registry.executionJobs.isEmpty then registry.empty.release(1)

    def executionJobs(registry: ExecutionJobRegistry) = registry.synchronized { registry.executionJobs }

    def waitEmpty(registry: ExecutionJobRegistry) = registry.empty.acquireAndRelease()
    def isEmpty(registry: ExecutionJobRegistry) = registry.empty.availablePermits() == 0


  class ExecutionJobRegistry:
    private var executionJobs = List[BatchExecutionJob]()
    private val empty = new Semaphore(1)

  def registryIsEmpty(environment: BatchEnvironment) = ExecutionJobRegistry.isEmpty(environment.registry)
  def waitJobKilled(environment: BatchEnvironment) = ExecutionJobRegistry.waitEmpty(environment.registry)

  def defaultUpdateInterval(implicit preference: Preference) =
    UpdateInterval(
      minUpdateInterval = preference(BatchEnvironment.MinUpdateInterval),
      maxUpdateInterval = preference(BatchEnvironment.MaxUpdateInterval),
      incrementUpdateInterval = preference(BatchEnvironment.IncrementUpdateInterval)
    )

  type REPLClassCache = AssociativeCache[Set[String], Seq[File]]


trait BatchEnvironment(val state: BatchEnvironmentState) extends SubmissionEnvironment:
  env =>

  def services: BatchEnvironment.Services

  def environmentPlugins: Iterable[File] = BatchEnvironment.environmentPlugins(this)
  def scriptPlugins: Iterable[File] = BatchEnvironment.scriptPlugins(services)

  def registry: ExecutionJobRegistry = state.registry
  def jobs: Seq[BatchExecutionJob] = ExecutionJobRegistry.executionJobs(registry)

  def submit(job: JobGroup) = BatchEnvironment.submit(this, job)(using services)

  def finishedJob(job: ExecutionJob) = {}
  def execute(batchExecutionJob: BatchExecutionJob)(using AccessControl.Priority): BatchJobControl
  def clean = BatchEnvironment.registryIsEmpty(env)

  def runtime = BatchEnvironment.runtimeLocation
  def jvmLinuxX64 = BatchEnvironment.JVMLinuxX64Location

  def error(e: ExceptionEvent) = state._errors.put(e)
  def errors: Seq[ExceptionEvent] = state._errors.elements
  def clearErrors: Seq[ExceptionEvent] = state._errors.clear()

  def ready: Long = BatchEnvironmentState.count(state, _ == ExecutionState.READY)
  def submitted: Long = BatchEnvironmentState.count(state, _ == ExecutionState.SUBMITTED)
  def running: Long = BatchEnvironmentState.count(state, _ == ExecutionState.RUNNING)

  def runningJobs = ExecutionJobRegistry.executionJobs(registry).filter(j => BatchEnvironment.executionSate(this, j) == ExecutionState.RUNNING)

  def done: Long = state._done.get()
  def failed: Long = state._failed.get()

  def stopped = state.stopped


object BatchEnvironmentState:
  def apply(services: BatchEnvironment.Services) =
    import services.newFile
    val _errors = new RingBuffer[ExceptionEvent](services.preference(Environment.maxExceptionsLog))
    val jobStore =
      val storeDirectory = TmpDirectory.makeNewDir("jobstore")
      JobStore(storeDirectory)

    new BatchEnvironmentState(_errors, jobStore)

  def getState(s: BatchEnvironmentState, id: Long) =
    s.jobState.synchronized:
      s.jobState(id)

  def putState(s: BatchEnvironmentState, id: Long, state: ExecutionState) =
    s.jobState.synchronized:
      s.jobState.put(id, state)

  def clearState(s: BatchEnvironmentState, id: Long) =
    s.jobState.synchronized:
      s.jobState.remove(id)

  def count(s: BatchEnvironmentState, f: ExecutionState => Boolean) =
    s.jobState.synchronized:
      s.jobState.count(j => f(j._2))

class BatchEnvironmentState(
  val _errors: RingBuffer[ExceptionEvent],
  val jobStore: JobStore):
  @volatile var stopped = false
  val registry = new ExecutionJobRegistry()
  val jobId = new AtomicLong(0L)
  val _done = new AtomicLong(0L)
  val _failed = new AtomicLong(0L)

  val jobState = collection.mutable.LongMap[ExecutionState]()