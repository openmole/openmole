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

package org.openmole.runtime.runtime

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.PrintStream
import java.util.UUID
import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.output.OutputManager
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.serializer.structure.PluginClassAndFiles
import org.openmole.tool.file._
import org.openmole.tool.hash._
import org.openmole.tool.tar._
import org.openmole.core.tools.service.{ Logger, LocalHostName, Retry }
import org.openmole.core.workspace.Workspace
import org.openmole.core.tools.service._
import org.openmole.core.batch.storage._
import org.openmole.core.batch.storage._
import org.openmole.core.workflow.execution.local._
import org.openmole.core.batch.message._
import org.openmole.core.serializer._
import org.openmole.core.pluginmanager._
import org.openmole.tool.tar.TarInputStream
import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import util.{ Success, Failure }
import org.openmole.core.workflow.execution.Environment.RuntimeLog

object Runtime extends Logger {
  val NbRetry = 3
  def retry[T](f: ⇒ T) = Retry.retry(f, NbRetry)
}

class Runtime {

  import Runtime._
  import Log._

  def apply(storage: RemoteStorage, communicationDirPath: String, inputMessagePath: String, outputMessagePath: String, debug: Boolean) = {

    /*--- get execution message and job for runtime---*/
    val usedFiles = new HashMap[String, File]

    logger.fine("Downloading input message")

    val executionMessage =
      Workspace.withTmpFile { executionMessageFileCache ⇒
        retry(storage.download(inputMessagePath, executionMessageFileCache))
        SerialiserService.deserialise[ExecutionMessage](executionMessageFileCache)
      }

    val oldOut = System.out
    val oldErr = System.err

    val out = Workspace.newFile("openmole", ".out")
    val err = Workspace.newFile("openmole", ".err")

    val outSt = new PrintStream(out)
    val errSt = new PrintStream(err)

    if (!debug) {
      OutputManager.redirectSystemOutput(outSt)
      OutputManager.redirectSystemError(errSt)
    }

    def getReplicatedFile(replicatedFile: ReplicatedFile, transferOptions: TransferOptions) =
      replicatedFile.download {
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
        } yield plugin -> getReplicatedFile(plugin, TransferOptions(raw = true))

      logger.fine("Plugins " + plugins.unzip._2)

      PluginManager.tryLoad(plugins.unzip._2)

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

      val runnableTasks = Workspace.withTmpFile { jobsFileCache ⇒
        logger.fine("Downloading execution message")
        retry(storage.download(executionMessage.jobs.path, jobsFileCache))
        SerialiserService.deserialiseReplaceFiles[Seq[RunnableTask]](jobsFileCache, usedFiles)
      }

      val saver = new ContextSaver(runnableTasks.size)
      val allMoleJobs = runnableTasks.map { _.toMoleJob(saver.save) }

      val beginExecutionTime = System.currentTimeMillis

      /* --- Submit all jobs to the local environment --*/
      logger.fine("Run the jobs")
      for (toProcess ← allMoleJobs) LocalEnvironment.default.submit(toProcess)

      saver.waitAllFinished

      val endExecutionTime = System.currentTimeMillis

      logger.fine("Results " + saver.results)

      val contextResults = ContextResults(saver.results)

      def uploadArchive =
        Workspace.withTmpFile { contextResultFile ⇒
          SerialiserService.serialiseAndArchiveFiles(contextResults, contextResultFile)
          val uploadedContextResults = storage.child(communicationDirPath, Storage.uniqName("contextResult", ".bin"))
          val contextResultFileHash = contextResultFile.hash.toString
          retry(storage.upload(contextResultFile, uploadedContextResults, TransferOptions(forceCopy = true, canMove = true)))
          ArchiveContextResults(FileMessage(uploadedContextResults, contextResultFileHash))
        }

      def uploadIndividualFiles =
        Workspace.withTmpFile { contextResultFile ⇒
          val PluginClassAndFiles(files, _) = SerialiserService.serialiseGetPluginsAndFiles(contextResults, contextResultFile)

          val replicated =
            files.map {
              _.upload {
                f ⇒
                  val name = storage.child(communicationDirPath, Storage.uniqName("resultFile", ".bin"))
                  retry(storage.upload(f, name, TransferOptions(forceCopy = true, canMove = true)))
                  name
              }
            }

          val uploadedContextResults = storage.child(communicationDirPath, Storage.uniqName("contextResult", ".bin"))
          val contextResultFileHash = contextResultFile.hash.toString

          retry(storage.upload(contextResultFile, uploadedContextResults, TransferOptions(forceCopy = true, canMove = true)))
          IndividualFilesContextResults(FileMessage(uploadedContextResults, contextResultFileHash), replicated)
        }

      val result =
        if (executionMessage.runtimeSettings.archiveResult) uploadArchive else uploadIndividualFiles

      val endTime = System.currentTimeMillis
      Success(result -> RuntimeLog(beginTime, beginExecutionTime, endExecutionTime, endTime))

    }
    catch {
      case t: Throwable ⇒
        if (debug) logger.log(SEVERE, "", t)
        Failure(t)
    }
    finally {
      outSt.close
      errSt.close

      System.setOut(oldOut)
      System.setErr(oldErr)
    }

    logger.fine("Upload the output")
    val outputMessage =
      if (out.length != 0) {
        val output = storage.child(executionMessage.communicationDirPath, Storage.uniqName("output", ".txt"))
        val outHash = out.hash.toString
        retry(storage.upload(out, output, TransferOptions(forceCopy = true, canMove = true)))
        Some(FileMessage(output, outHash))
      }
      else None

    out.delete

    val errorMessage =
      if (err.length != 0) {
        val errout = storage.child(executionMessage.communicationDirPath, Storage.uniqName("outputError", ".txt"))
        val errHash = err.hash.toString
        retry(storage.upload(err, errout, TransferOptions(forceCopy = true, canMove = true)))
        Some(FileMessage(errout, errHash))
      }
      else None

    err.delete

    val runtimeResult = RuntimeResult(outputMessage, errorMessage, result, localRuntimeInfo)

    logger.fine("Upload the result message")
    Workspace.withTmpFile("output", ".res") { outputLocal ⇒
      SerialiserService.serialise(runtimeResult, outputLocal)
      retry(storage.upload(outputLocal, outputMessagePath, TransferOptions(forceCopy = true, canMove = true)))
    }

    result
  }

}
