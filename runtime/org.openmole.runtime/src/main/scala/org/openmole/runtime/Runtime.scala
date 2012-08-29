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

import com.ice.tar.TarEntry
import com.ice.tar.TarInputStream
import com.ice.tar.TarOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.PrintStream
import java.util.UUID
import org.openmole.misc.eventdispatcher._
import org.openmole.misc.exception._
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.tools.io.TarArchiver._
import org.openmole.misc.tools.service._
import org.openmole.core.batch.authentication._
import org.openmole.core.batch.file._
import org.openmole.core.implementation.execution.local._
import org.openmole.core.batch.message._
import org.openmole.misc.tools.service.Retry._
import org.openmole.core.serializer._
import org.openmole.misc.hashservice._
import org.openmole.misc.pluginmanager._
import org.openmole.misc.workspace._
import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap

object Runtime extends Logger {
  val NbRetry = 3
}

class Runtime {

  import Runtime._

  def apply(baseURI: String, communicationDirPath: String, executionMessageURI: String, resultMessageURI: String, debug: Boolean) = {

    val path = new RelativePath(baseURI)

    val oldOut = System.out
    val oldErr = System.err

    val out = Workspace.newFile("openmole", ".out")
    val err = Workspace.newFile("openmole", ".err")

    val outSt = new PrintStream(out)
    val errSt = new PrintStream(err)

    if (!debug) {
      System.setOut(outSt)
      System.setErr(errSt)
    }

    /*--- get execution message and job for runtime---*/
    val usedFiles = new HashMap[File, File]

    val executionMesageFileCache = path.cacheUnziped(executionMessageURI)
    val executionMessage = SerializerService.deserialize[ExecutionMessage](executionMesageFileCache)
    executionMesageFileCache.delete

    val result = try {
      val pluginDir = Workspace.newDir

      for (plugin ← executionMessage.plugins) {
        val inPluginDirLocalFile = File.createTempFile("plugin", ".jar", pluginDir)
        val replicaFileCache = path.toGZURIFile(plugin.replicaPath).copy(inPluginDirLocalFile)

        if (HashService.computeHash(inPluginDirLocalFile) != plugin.hash)
          throw new InternalProcessingError("Hash of a plugin does't match.")

        usedFiles.put(plugin.src, inPluginDirLocalFile)
      }

      PluginManager.loadDir(pluginDir)

      /* --- Download the files for the local file cache ---*/

      for (repliURI ← executionMessage.files) {

        //To avoid getting twice the same plugin with different path
        if (!usedFiles.containsKey(repliURI.src)) {

          val cache = path.cacheUnziped(repliURI.replicaPath)
          val cacheHash = HashService.computeHash(cache).toString

          if (cacheHash != repliURI.hash)
            throw new InternalProcessingError("Hash is incorrect for file " + repliURI.src.toString + " replicated at " + path.toStringURI(repliURI.replicaPath))

          val local = if (repliURI.directory) {
            val local = Workspace.newDir("dirReplica")
            cache.extractDirArchiveWithRelativePath(local)
            local
          } else {
            cache.mode = repliURI.mode
            cache
          }

          usedFiles.put(repliURI.src, local)
        }
      }

      val jobsFileCache = path.cacheUnziped(executionMessage.jobs.path)

      if (HashService.computeHash(jobsFileCache).toString != executionMessage.jobs.hash) throw new InternalProcessingError("Hash of the execution job does't match.")

      val tis = new TarInputStream(new FileInputStream(jobsFileCache))
      val runableTasks = tis.applyAndClose(e ⇒ { SerializerService.deserializeReplaceFiles[RunnableTask](tis, usedFiles) })
      jobsFileCache.delete

      val saver = new ContextSaver(runableTasks.size)
      val allMoleJobs = runableTasks.map { _.toMoleJob(saver.save) }

      /* --- Submit all jobs to the local environment --*/
      for (toProcess ← allMoleJobs) LocalEnvironment.default.submit(toProcess)

      saver.waitAllFinished

      logger.fine("Result " + saver.results)

      val contextResults = new ContextResults(saver.results)
      val contextResultFile = Workspace.newFile

      SerializerService.serializeAndArchiveFiles(contextResults, contextResultFile)
      val uploadedcontextResults = new GZURIFile(path.toURIFile(executionMessage.communicationDirPath).newFileInDir("uplodedTar", ".tgz"))
      val result = new FileMessage(uploadedcontextResults.path, HashService.computeHash(contextResultFile).toString)
      retry(URIFile.copy(contextResultFile, uploadedcontextResults), NbRetry)
      contextResultFile.delete
      Left(result)

    } catch {
      case t ⇒ {
        if (debug) logger.log(SEVERE, "", t)
        Right(t)
      }
    } finally {
      outSt.close
      errSt.close

      System.setOut(oldOut)
      System.setErr(oldErr)
    }

    val outputMessage =
      if (out.length != 0) {
        val output = new GZURIFile(path.toURIFile(executionMessage.communicationDirPath).newFileInDir("output", ".txt"))
        URIFile.copy(out, output)
        Some(new FileMessage(output.path, HashService.computeHash(out).toString))
      } else None

    out.delete

    val errorMessage =
      if (err.length != 0) {
        val errout = new GZURIFile(path.toURIFile(executionMessage.communicationDirPath).newFileInDir("outputError", ".txt"))
        URIFile.copy(err, errout)
        Some(new FileMessage(errout.path, HashService.computeHash(err).toString))
      } else None

    err.delete

    val runtimeResult = new RuntimeResult(outputMessage, errorMessage, result)

    val outputLocal = Workspace.newFile("output", ".res")
    SerializerService.serialize(runtimeResult, outputLocal)
    try {
      val output = path.toGZURIFile(resultMessageURI)
      retry(URIFile.copy(outputLocal, output), NbRetry)
    } finally outputLocal.delete
  }

}
