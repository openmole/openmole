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
import org.openmole.core.batch.storage._
import org.openmole.core.implementation.execution.local._
import org.openmole.core.batch.message._
import org.openmole.misc.tools.service.Retry
import org.openmole.core.serializer._
import org.openmole.misc.hashservice._
import org.openmole.misc.pluginmanager._
import org.openmole.misc.workspace._
import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import util.{ Success, Failure }

object Runtime extends Logger {
  val NbRetry = 3

  def retry[T](f: ⇒ T) = Retry.retry(f, NbRetry)
}

class Runtime {

  import Runtime._

  def apply(storage: SimpleStorage, communicationDirPath: String, inputMessagePath: String, outputMessagePath: String, debug: Boolean) = {

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

    val executionMessage = retry(Workspace.withTmpFile { executionMesageFileCache ⇒
      storage.downloadGZ(inputMessagePath, executionMesageFileCache)
      SerializerService.deserialize[ExecutionMessage](executionMesageFileCache)
    })

    val result = try {
      val pluginDir = Workspace.newDir

      for (plugin ← executionMessage.plugins) {
        val inPluginDirLocalFile = File.createTempFile("plugin", ".jar", pluginDir)
        storage.downloadGZ(plugin.path, inPluginDirLocalFile)

        if (HashService.computeHash(inPluginDirLocalFile).toString != plugin.hash)
          throw new InternalProcessingError("Hash of a plugin does't match.")

        usedFiles.put(plugin.src, inPluginDirLocalFile)
      }

      PluginManager.loadDir(pluginDir)

      /* --- Download the files for the local file cache ---*/

      for (repliURI ← executionMessage.files) {

        //To avoid getting twice the same plugin with different path
        if (!usedFiles.containsKey(repliURI.src)) {
          val cache = Workspace.newFile
          storage.downloadGZ(repliURI.path, cache)
          val cacheHash = HashService.computeHash(cache).toString

          if (cacheHash != repliURI.hash)
            throw new InternalProcessingError("Hash is incorrect for file " + repliURI.src.toString + " replicated at " + repliURI.path)

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

      val jobsFileCache = Workspace.newFile
      storage.downloadGZ(executionMessage.jobs.path, jobsFileCache)

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
      val uploadedcontextResults = storage.child(executionMessage.communicationDirPath, Storage.uniqName("uplodedTar", ".tgz"))
      val result = new FileMessage(uploadedcontextResults, HashService.computeHash(contextResultFile).toString)
      retry(storage.uploadGZ(contextResultFile, uploadedcontextResults))
      contextResultFile.delete
      Success(result)
    } catch {
      case t: Throwable ⇒
        if (debug) logger.log(SEVERE, "", t)
        Failure(t)
    } finally {
      outSt.close
      errSt.close

      System.setOut(oldOut)
      System.setErr(oldErr)
    }

    val outputMessage =
      if (out.length != 0) {
        val output = storage.child(executionMessage.communicationDirPath, Storage.uniqName("output", ".txt"))
        storage.uploadGZ(out, output)
        Some(new FileMessage(output, HashService.computeHash(out).toString))
      } else None

    out.delete

    val errorMessage =
      if (err.length != 0) {
        val errout = storage.child(executionMessage.communicationDirPath, Storage.uniqName("outputError", ".txt"))
        storage.uploadGZ(err, errout)
        Some(new FileMessage(errout, HashService.computeHash(err).toString))
      } else None

    err.delete

    val runtimeResult = new RuntimeResult(outputMessage, errorMessage, result)

    val outputLocal = Workspace.newFile("output", ".res")
    SerializerService.serialize(runtimeResult, outputLocal)
    try retry(storage.uploadGZ(outputLocal, outputMessagePath))
    finally outputLocal.delete
  }

}
