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
import java.nio.file.Files
import java.util.UUID

import org.openmole.core.communication.message._
import org.openmole.core.communication.storage.{RemoteStorage, TransferOptions}
import org.openmole.core.event.{Event, EventDispatcher}
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.fileservice.{FileCache, FileService, FileServiceCache}
import org.openmole.core.location._
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.preference.{ConfigurationLocation, Preference}
import org.openmole.core.replication.ReplicaCatalog
import org.openmole.core.serializer.{PluginAndFilesListing, SerializerService}
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.workflow.execution._
import org.openmole.core.workflow.job._
import org.openmole.core.workspace._
import org.openmole.plugin.environment.batch.environment.BatchEnvironment.ExecutionJobRegistry
import org.openmole.plugin.environment.batch.refresh._
import org.openmole.tool.bytecode.listAllClasses
import org.openmole.tool.cache._
import org.openmole.tool.file._
import org.openmole.tool.logger.JavaLogger
import org.openmole.tool.osgi._
import org.openmole.tool.random.{RandomProvider, Seeder, shuffled}
import squants.information.Information
import squants.information.InformationConversions._
import squants.time.TimeConversions._

import scala.collection.immutable.TreeSet
import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader

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

  def jobFiles(job: BatchExecutionJob) =
    job.pluginsAndFiles.files.toVector ++
      job.pluginsAndFiles.plugins ++
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

    val plugins = new TreeSet[File]()(fileOrdering) ++ job.plugins -- job.environment.plugins
    val files = (new TreeSet[File]()(fileOrdering) ++ job.files) diff plugins

    val runtime = replicateTheRuntime(job.job, job.environment, replicate)

    val executionMessage = createExecutionMessage(
      job.job,
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
    job:              Job,
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
    job:                 Job,
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
      def pruneFinishedJobs(registry: ExecutionJobRegistry) = registry.executionJobs = registry.executionJobs.filter(_.state != ExecutionState.KILLED)
      pruneFinishedJobs(registry)
    }

    def executionJobs(registry: ExecutionJobRegistry) = registry.synchronized { registry.executionJobs }
    def numberOfExecutionJobs(registry: ExecutionJobRegistry, job: Job) = registry.synchronized {
      registry.executionJobs.count(_.job == job)
    }

    def lonelyJobs(registry: ExecutionJobRegistry) = registry.synchronized {
      registry.executionJobs.view.groupBy(_.job).filter(j => !j._1.finished && j._2.isEmpty).unzip._1.toSeq
    }
  }

  class ExecutionJobRegistry {
    var executionJobs = List[BatchExecutionJob]()
  }

  def defaultUpdateInterval(implicit preference: Preference) =
    UpdateInterval(
      minUpdateInterval = preference(BatchEnvironment.MinUpdateInterval),
      maxUpdateInterval = preference(BatchEnvironment.MaxUpdateInterval),
      incrementUpdateInterval = preference(BatchEnvironment.IncrementUpdateInterval)
    )
}

abstract class BatchEnvironment extends SubmissionEnvironment { env ⇒

  implicit val services: BatchEnvironment.Services
  def eventDispatcherService = services.eventDispatcher

  def exceptions = services.preference(Environment.maxExceptionsLog)

  def clean = BatchEnvironment.isClean(this)

  lazy val registry = new ExecutionJobRegistry()
  def jobs = ExecutionJobRegistry.executionJobs(registry)

  lazy val relpClassesCache = new AssociativeCache[Set[String], (Seq[File], Seq[FileCache])]

  lazy val plugins = PluginManager.pluginsForClass(this.getClass)

  override def submit(job: Job) = {
    val bej = BatchExecutionJob(job, this)
    ExecutionJobRegistry.register(registry, bej)
    eventDispatcherService.trigger(this, new Environment.JobSubmitted(bej))
    JobManager ! Manage(bej)
  }

  def execute(batchExecutionJob: BatchExecutionJob): BatchJobControl

  def runtime = BatchEnvironment.runtimeLocation
  def jvmLinuxX64 = BatchEnvironment.JVMLinuxX64Location

  def submitted: Long = jobs.count { _.state == ExecutionState.SUBMITTED }
  def running: Long = jobs.count { _.state == ExecutionState.RUNNING }

  def runtimeSettings = RuntimeSettings(archiveResult = false)

  def finishedJob(job: ExecutionJob) = {}

}

object BatchExecutionJob {
  def apply(job: Job, environment: BatchEnvironment) = new BatchExecutionJob(job, environment)

  def toClassPath(c: String) = s"${c.replace('.', '/')}.class"
  def toClassName(p: String) = p.dropRight(".class".size).replace("/", ".")

  def replClassDirectory(c: Class[_]) = {
    val replClassloader = c.getClassLoader.asInstanceOf[URLClassLoader]
    val location = toClassPath(c.getName)
    val classURL = replClassloader.findResource(location)
    new File(classURL.getPath.dropRight(location.size))
  }

  def allClasses(directory: File): Seq[ClassFile] = {
    import java.nio.file._

    import collection.JavaConverters._
    Files.walk(directory.toPath).
      filter(p => Files.isRegularFile(p) && p.toFile.getName.endsWith(".class")).iterator().asScala.
      map { p =>
        val path = directory.toPath.relativize(p)
        ClassFile(path.toString, p.toFile)
      }.toList
  }

  case class ClosuresBundle(classes: Seq[ClassFile], exported: Seq[String], dependencies: Seq[VersionedPackage], plugins: Seq[File])

  def replClassesToPlugins(replClasses: Seq[Class[_]])(implicit newFile: NewFile, fileService: FileService) = {
    val replDirectories = replClasses.map(c => c.getClassLoader -> BatchExecutionJob.replClassDirectory(c)).distinct

    def bundle(directory: File, classLoader: ClassLoader) = {
      val allClassFiles = BatchExecutionJob.allClasses(directory)

      val mentionedClasses =
        for {
          f <- allClassFiles.toList
          t <- listAllClasses(Files.readAllBytes(f.file))
          c <- util.Try[Class[_]](Class.forName(t.getClassName, false, classLoader)).toOption.toSeq
        } yield c

      def toVersionedPackage(c: Class[_]) = {
        val p = c.getName.reverse.dropWhile(_ != '.').drop(1).reverse
        PluginManager.bundleForClass(c).map { b => VersionedPackage(p, Some(b.getVersion.toString)) }
      }

      val packages = mentionedClasses.flatMap(toVersionedPackage).distinct
      val plugins = mentionedClasses.flatMap(PluginManager.pluginsForClass)

      val exported =
        allClassFiles.flatMap(c => Option(new File(c.path).getParent)).distinct.
          filter(PluginAndFilesListing.looksLikeREPLClassName).
          map(_.replace("/", "."))

      val replClassFiles = allClassFiles.filter(c => PluginAndFilesListing.looksLikeREPLClassName(c.path.replace("/", ".")))

      BatchExecutionJob.ClosuresBundle(replClassFiles, exported, packages, plugins)
    }

    def bundleFile(closures: ClosuresBundle) = {
      val bundle = newFile.newFile("closureBundle", ".jar")
      try createBundle("closure-" + UUID.randomUUID.toString, "1.0", closures.classes, closures.exported, closures.dependencies, bundle)
      catch {
        case e: Throwable ⇒
          bundle.delete()
          throw e
      }
      FileCache(bundle)(fileService)
    }

    val (bfs, plugins) =
      replDirectories.map {
        case (c, d) =>
          val b = bundle(d, c)
          (bundleFile(b), b.plugins)
      }.unzip

    // bfs is kept to avoid garbage collection of file caches
    (bfs.map(_.file) ++ plugins.flatten.toList.distinct, bfs)
  }


}

class BatchExecutionJob(val job: Job, val environment: BatchEnvironment) extends ExecutionJob { bej ⇒


  def moleJobs = job.moleJobs
  def runnableTasks = job.moleJobs.map(RunnableTask(_))

  @transient lazy val plugins = pluginsAndFiles.plugins ++ closureBundleAndPlugins._1
  def files = pluginsAndFiles.files

  @transient lazy val pluginsAndFiles = environment.services.serializerService.pluginsAndFiles(runnableTasks)

  def closureBundleAndPlugins = {
    import environment.services._
    val replClasses = pluginsAndFiles.replClasses
    environment.relpClassesCache.cache(job.moleExecution, pluginsAndFiles.replClasses.map(_.getName).toSet, preCompute = false) { _ =>
      BatchExecutionJob.replClassesToPlugins(replClasses)
    }
  }

  def usedFiles: Iterable[File] =
    (files ++
      Seq(environment.runtime, environment.jvmLinuxX64) ++
      environment.plugins ++ plugins).distinct

  def usedFileHashes = usedFiles.map(f ⇒ (f, environment.services.fileService.hash(f)(environment.services.newFile, environment.services.fileServiceCache)))

}