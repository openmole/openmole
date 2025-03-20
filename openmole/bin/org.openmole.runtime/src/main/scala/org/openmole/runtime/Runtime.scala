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

package org.openmole.runtime

import java.io.File
import java.io.PrintStream
import org.openmole.core.exception.{InternalProcessingError, MultipleException}
import org.openmole.core.outputmanager.OutputManager
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.workflow.task.TaskExecutionContext
import org.openmole.tool.logger.{JavaLogger, LoggerService}
import org.openmole.core.workspace.{TmpDirectory, Workspace}
import org.openmole.core.workflow.execution.*
import org.openmole.core.communication.message.*
import org.openmole.core.communication.storage.*
import org.openmole.core.event.EventDispatcher
import org.openmole.core.fileservice.{FileService, FileServiceCache}
import org.openmole.core.networkservice.NetworkService
import org.openmole.core.preference.Preference
import org.openmole.core.serializer.*
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.timeservice.TimeService
import org.openmole.tool.file.uniqName
import org.openmole.tool.system.*
import scala.jdk.CollectionConverters.*
import scala.collection.mutable.HashMap
import util.{Failure, Success}
import org.openmole.core.workflow.execution.Environment.RuntimeLog
import org.openmole.core.workflow.job.Job
import org.openmole.tool.cache.KeyValueCache
import org.openmole.tool.exception.Retry
import org.openmole.tool.lock.*
import org.openmole.tool.outputredirection.OutputRedirection
import org.openmole.tool.stream.MultiplexedOutputStream
import squants.*

object Runtime extends JavaLogger:

  import squants.time.TimeConversions._

  def retry[T](f: => T, retry: Option[Int]) =
    retry match
      case None    => f
      case Some(r) => Retry.retry(f, r, Some(1 seconds))

  def signalHandler(env: LocalEnvironment)(using tp: ThreadProvider): Signal.Handler =
    si =>
      Log.logger.warning(s"Received signal $si, shutting down")
      import org.openmole.tool.thread.*
      tp.virtual: () =>
        try env.stop()
        finally
          Thread.sleep(20.seconds.millis)
          Log.logger.warning(s"Waiting 20 seconds before killing the JVM")
          System.exit(si.getNumber)

class Runtime:

  import Runtime._
  import Log._

  def apply(
    storage:           RemoteStorage,
    inputMessagePath:  String,
    outputMessagePath: String,
    threads:           Int,
    debug:             Boolean,
    transferRetry:     Option[Int]
  )(implicit serializerService: SerializerService, newFile: TmpDirectory, fileService: FileService, fileServiceCache: FileServiceCache, preference: Preference, threadProvider: ThreadProvider, eventDispatcher: EventDispatcher, workspace: Workspace, loggerService: LoggerService, networkService: NetworkService, timeService: TimeService) =

    /*--- get execution message and job for runtime---*/
    val usedFiles = new HashMap[String, File]

    logger.fine("Downloading input message")

    val executionMessage =
      newFile.withTmpFile: executionMessageFileCache =>
        retry(storage.download(inputMessagePath, executionMessageFileCache), transferRetry)
        serializerService.deserializeAndExtractFiles[ExecutionMessage](executionMessageFileCache, deleteFilesOnGC = true, gz = true)

    val systemOut = OutputManager.systemOutput
    val systemErr = OutputManager.systemError

    val out = newFile.newFile("openmole", ".out")
    val outSt = new PrintStream(out)

    val multiplexedOut = new PrintStream(MultiplexedOutputStream(outSt, systemOut), true)
    val multiplexedErr = new PrintStream(MultiplexedOutputStream(outSt, systemErr), true)

    OutputManager.redirectSystemOutput(multiplexedOut)
    OutputManager.redirectSystemError(multiplexedErr)

    val outputRedirection = OutputRedirection(multiplexedOut)

    def getReplicatedFile(replicatedFile: ReplicatedFile, transferOptions: TransferOptions) =
      ReplicatedFile.download(replicatedFile):
        (path, file) =>
          try retry(storage.download(path, file, transferOptions), transferRetry)
          catch
            case e: Exception => throw new InternalProcessingError(s"Error downloading $replicatedFile", e)

    val beginTime = System.currentTimeMillis

    val environment = new LocalEnvironment(threads = threads, false, Some("runtime local"), remote = true)
    environment.start()

    Signal.registerSignalCatcher(Seq("TERM"))(Runtime.signalHandler(environment))
    
    val result =
      try
        logger.fine("Downloading plugins")

        //val pluginDir = Workspace.newDir

        val plugins =
          for
            plugin ← executionMessage.plugins
          yield
            val pluginFile = getReplicatedFile(plugin, TransferOptions(raw = true))
            plugin → pluginFile

        logger.fine("Downloaded plugins. " + plugins.unzip._2.mkString(", "))

        PluginManager.tryLoad(plugins.unzip._2).foreach { case (f, e) => logger.log(WARNING, s"Error loading bundle $f", e) }

        logger.fine("Loaded plugins: " + PluginManager.bundles.map(_.getSymbolicName).mkString(", "))

        for
          (p, f) ← plugins
        do usedFiles.put(p.originalPath, f)

        /* --- Download the files for the local file cache ---*/
        logger.fine("Downloading files")

        for
          repliURI <- executionMessage.files
        do
          // To avoid getting twice the same plugin
          if !usedFiles.contains(repliURI.originalPath)
          then
            val local = getReplicatedFile(repliURI, TransferOptions())
            usedFiles.put(repliURI.originalPath, local)

        val runnableTasks = serializerService.deserializeReplaceFiles[RunnableTaskSequence](executionMessage.jobs, Map() ++ usedFiles, gz = true)

        val saver = new ContextSaver(runnableTasks.size)
        val callBack = Job.CallBack(saver.save, () => false)
        val allMoleJobs = runnableTasks.map(t => Job(t.task, t.context, t.id, callBack))

        val beginExecutionTime = System.currentTimeMillis

        /* --- Submit all jobs to the local environment --*/
        logger.fine("Run the jobs")

        try
          val taskExecutionContext = TaskExecutionContext.partial(
            applicationExecutionDirectory = newFile.makeNewDir("application"),
            moleExecutionDirectory = newFile.makeNewDir("runtime"),
            preference = preference,
            threadProvider = threadProvider,
            fileService = fileService,
            fileServiceCache = fileServiceCache,
            workspace = workspace,
            outputRedirection = outputRedirection,
            loggerService = loggerService,
            cache = KeyValueCache(),
            lockRepository = LockRepository[LockKey](),
            serializerService = serializerService,
            networkService = networkService,
            timeService = timeService
          )

          for
            toProcess <- allMoleJobs
          do
            environment.submit(toProcess, taskExecutionContext)

          saver.waitAllFinished
        finally environment.stop()

        val endExecutionTime = System.currentTimeMillis

        val results = saver.results
        logger.fine("Results " + results)

        if results.values.forall(_.isFailure)
        then
          val failures = results.values.collect { case Failure(e) => e }
          throw new InternalProcessingError("All mole job executions have failed", MultipleException(failures))

        val contextResults = ContextResults(results)

        def uploadArchive =
          val contextResultFile = fileService.wrapRemoveOnGC(newFile.newFile("contextResult", "res"))
          serializerService.serializeAndArchiveFiles(contextResults, contextResultFile, gz = true)
          ArchiveContextResults(contextResultFile)

        def uploadIndividualFiles =
          val contextResultFile = fileService.wrapRemoveOnGC(newFile.newFile("contextResult", "res"))
          serializerService.serialize(contextResults, contextResultFile, gz = true)
          val files = serializerService.listFiles(contextResults)

          val replicated =
            files.map: file =>
              def uploadOnStorage(f: File) = retry(storage.upload(f, None, TransferOptions(noLink = true, canMove = true)), transferRetry)
              ReplicatedFile.upload(file, uploadOnStorage)

          IndividualFilesContextResults(contextResultFile, replicated)

        val result =
          if (executionMessage.runtimeSettings.archiveResult) uploadArchive else uploadIndividualFiles

        val endTime = System.currentTimeMillis
        Success(result → RuntimeLog(beginTime, beginExecutionTime, endExecutionTime, endTime))
      catch
        case t: Throwable =>
          if (debug) logger.log(SEVERE, "", t)
          Failure(t)
      finally
        multiplexedOut.close()
        multiplexedErr.close()
        outSt.close()
        OutputManager.uninstall

    val outputMessage = if (out.length != 0) Some(out) else None

    val runtimeResult = RuntimeResult(outputMessage, result, RuntimeInfo.localRuntimeInfo)

    newFile.withTmpFile("output", ".tgz"): outputLocal =>
      logger.fine(s"Serializing result to $outputLocal")
      serializerService.serializeAndArchiveFiles(runtimeResult, outputLocal, gz = true)
      logger.fine(s"Upload the serialized result to $outputMessagePath on $storage")
      retry(storage.upload(outputLocal, Some(outputMessagePath), TransferOptions(noLink = true, canMove = true)), transferRetry)

    result

