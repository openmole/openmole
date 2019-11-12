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

import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.outputmanager.OutputManager
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.workflow.task.TaskExecutionContext
import org.openmole.tool.logger.{ JavaLogger, LoggerService }
import org.openmole.core.tools.service.Retry
import org.openmole.core.workspace.{ NewFile, Workspace }
import org.openmole.core.tools.service._
import org.openmole.core.workflow.execution._
import org.openmole.core.communication.message._
import org.openmole.core.communication.storage._
import org.openmole.core.event.EventDispatcher
import org.openmole.core.fileservice.FileService
import org.openmole.core.preference.Preference
import org.openmole.core.serializer._
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.tool.file.uniqName

import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import util.{ Failure, Success }
import org.openmole.core.workflow.execution.Environment.RuntimeLog
import org.openmole.core.workflow.job.MoleJob
import org.openmole.tool.cache.KeyValueCache
import org.openmole.tool.lock._
import org.openmole.tool.outputredirection.OutputRedirection
import squants._

object Runtime extends JavaLogger {
  val NbRetry = 3
  def retry[T](f: ⇒ T, coolDown: Option[Time] = None) = Retry.retry(f, NbRetry, coolDown)
}

class Runtime {

  import Runtime._
  import Log._

  def apply(
    storage:           RemoteStorage,
    inputMessagePath:  String,
    outputMessagePath: String,
    threads:           Int,
    debug:             Boolean
  )(implicit serializerService: SerializerService, newFile: NewFile, fileService: FileService, preference: Preference, threadProvider: ThreadProvider, eventDispatcher: EventDispatcher, workspace: Workspace, loggerService: LoggerService) = {

    /*--- get execution message and job for runtime---*/
    val usedFiles = new HashMap[String, File]

    logger.fine("Downloading input message")

    val (executionMessage, _) =
      newFile.withTmpFile { executionMessageFileCache ⇒
        retry(storage.download(inputMessagePath, executionMessageFileCache))
        ExecutionMessage.load(executionMessageFileCache)
      }

    val oldOut = System.out
    val oldErr = System.err

    val out = newFile.newFile("openmole", ".out")
    val outSt = new PrintStream(out)

    if (!debug) {
      OutputManager.redirectSystemOutput(outSt)
      OutputManager.redirectSystemError(outSt)
    }

    val outputRedirection = if (!debug) OutputRedirection(outSt) else OutputRedirection(System.out, System.err)

    def getReplicatedFile(replicatedFile: ReplicatedFile, transferOptions: TransferOptions) =
      ReplicatedFile.download(replicatedFile) {
        (path, file) ⇒
          try retry(storage.download(path, file, transferOptions))
          catch {
            case e: Exception ⇒ throw new InternalProcessingError(s"Error downloading $replicatedFile", e)
          }
      }

    val beginTime = System.currentTimeMillis

    val result = try {
      logger.fine("Downloading plugins")

      //val pluginDir = Workspace.newDir

      val plugins =
        for {
          plugin ← executionMessage.plugins
        } yield {
          val pluginFile = getReplicatedFile(plugin, TransferOptions(raw = true))
          plugin → pluginFile
        }

      logger.fine("Downloaded plugins. " + plugins.unzip._2.mkString(", "))

      PluginManager.tryLoad(plugins.unzip._2).foreach { case (f, e) ⇒ logger.log(WARNING, s"Error loading bundle $f", e) }

      logger.fine("Loaded plugins: " + PluginManager.bundles.map(_.getSymbolicName).mkString(", "))

      for { (p, f) ← plugins } usedFiles.put(p.originalPath, f)

      /* --- Download the files for the local file cache ---*/
      logger.fine("Downloading files")

      for (repliURI ← executionMessage.files) {
        // To avoid getting twice the same plugin
        if (!usedFiles.containsKey(repliURI.originalPath)) {
          val local = getReplicatedFile(repliURI, TransferOptions())
          usedFiles.put(repliURI.originalPath, local)
        }
      }

      val runnableTasks = serializerService.deserializeReplaceFiles[Seq[RunnableTask]](executionMessage.jobs, usedFiles)

      val saver = new ContextSaver(runnableTasks.size)
      val allMoleJobs = runnableTasks.map { t ⇒ MoleJob(t.task, t.context, t.id, saver.save, () ⇒ false) }

      val beginExecutionTime = System.currentTimeMillis

      /* --- Submit all jobs to the local environment --*/
      logger.fine("Run the jobs")
      val environment = new LocalEnvironment(nbThreads = threads, false, Some("runtime local"))
      environment.start()

      try {

        val taskExecutionContext = TaskExecutionContext(
          moleExecutionDirectory = newFile.makeNewDir("runtime"),
          taskExecutionDirectory = newFile.makeNewDir("task"),
          localEnvironment = environment,
          preference = preference,
          threadProvider = threadProvider,
          fileService = fileService,
          workspace = workspace,
          outputRedirection = outputRedirection,
          loggerService = loggerService,
          cache = KeyValueCache(),
          lockRepository = LockRepository[LockKey]())

        for (toProcess ← allMoleJobs) environment.submit(toProcess, taskExecutionContext)
        saver.waitAllFinished
      }
      finally environment.stop()

      val endExecutionTime = System.currentTimeMillis

      logger.fine("Results " + saver.results)

      val contextResults = ContextResults(saver.results)

      def uploadArchive = {
        val contextResultFile = newFile.newFile("contextResult", "res")
        serializerService.serializeAndArchiveFiles(contextResults, contextResultFile)
        fileService.deleteWhenGarbageCollected(contextResultFile)
        ArchiveContextResults(contextResultFile)
      }

      def uploadIndividualFiles = {
        val contextResultFile = newFile.newFile("contextResult", "res")
        serializerService.serialize(contextResults, contextResultFile)
        fileService.deleteWhenGarbageCollected(contextResultFile)
        val pac = serializerService.pluginsAndFiles(contextResults)

        val replicated =
          pac.files.map { file ⇒
            def uploadOnStorage(f: File) = retry(storage.upload(f, None, TransferOptions(noLink = true, canMove = true)))
            ReplicatedFile.upload(file, uploadOnStorage)
          }

        IndividualFilesContextResults(contextResultFile, replicated)
      }

      val result =
        if (executionMessage.runtimeSettings.archiveResult) uploadArchive else uploadIndividualFiles

      val endTime = System.currentTimeMillis
      Success(result → RuntimeLog(beginTime, beginExecutionTime, endExecutionTime, endTime))

    }
    catch {
      case t: Throwable ⇒
        if (debug) logger.log(SEVERE, "", t)
        Failure(t)
    }
    finally {
      outSt.close
      System.setOut(oldOut)
      System.setErr(oldErr)
    }

    val outputMessage = if (out.length != 0) Some(out) else None

    val runtimeResult = RuntimeResult(outputMessage, result, RuntimeInfo.localRuntimeInfo)

    newFile.withTmpFile("output", ".tgz") { outputLocal ⇒
      logger.fine(s"Serializing result to $outputLocal")
      serializerService.serializeAndArchiveFiles(runtimeResult, outputLocal)
      logger.fine(s"Upload the serialized result to $outputMessagePath on $storage")
      retry(storage.upload(outputLocal, Some(outputMessagePath), TransferOptions(noLink = true, canMove = true)))
    }

    result
  }

}
