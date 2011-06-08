/*
 * Copyright (C) 2010 reuillon
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
import java.util.logging.Level
import java.util.logging.Logger
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.tools.io.TarArchiver._
import org.openmole.misc.tools.service.Priority
import org.openmole.core.batch.file.GZURIFile
import org.openmole.core.batch.file.URIFile
import org.openmole.core.implementation.execution.local.LocalExecutionEnvironment
import org.openmole.core.batch.message.ContextResults
import org.openmole.core.batch.message.ExecutionMessage
import org.openmole.core.batch.message.FileMessage
import org.openmole.core.batch.message.RuntimeResult
import org.openmole.core.batch.file.IURIFile
import org.openmole.core.model.job.IMoleJob
import org.openmole.misc.tools.service.Retry._
import org.openmole.core.serializer.SerializerService
import org.openmole.misc.hashservice.HashService
import org.openmole.misc.pluginmanager.PluginManager
import org.openmole.misc.workspace.Workspace
import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap

object Runtime {
  val NumberOfLocalTheads = 1
  val NbRetry = 3
}

class Runtime {

  import Runtime._

  def apply(executionMessageURI: String, resultMessageURI: String, debug: Boolean) = {
    
    val oldOut = System.out
    val oldErr = System.err

    val out = Workspace.newFile("openmole", ".out")
    val err = Workspace.newFile("openmole", ".err")

    val outSt = new PrintStream(out)
    val errSt = new PrintStream(err)

    if(!debug) {
      System.setOut(outSt)
      System.setErr(errSt)
    }
        
    var exception: Throwable = null
    var outputMessage: FileMessage = null
    var errorMessage: FileMessage = null
    var tarResultMessage: FileMessage = null
        
    val filesInfo = new HashMap[String, (File, Boolean)]
    var contextResult: IURIFile = null
        
    try {
      Workspace.setPreference(LocalExecutionEnvironment.DefaultNumberOfThreads, Integer.toString(NumberOfLocalTheads));
                        
      /*--- get execution message and job for runtime---*/
      val usedFiles = new HashMap[File, File]
            
      val executionMessageFile = new GZURIFile(new URIFile(executionMessageURI));
      val executionMesageFileCache = executionMessageFile.cache
      val executionMessage = SerializerService.deserialize[ExecutionMessage](executionMesageFileCache)
      executionMesageFileCache.delete
            
      val pluginDir = Workspace.newDir

      for (plugin <- executionMessage.plugins) {
        val replicaFileCache = plugin.replica.cache

        val inPluginDirLocalFile = File.createTempFile("plugin", ".jar", pluginDir)
        replicaFileCache.renameTo(inPluginDirLocalFile)

        if (HashService.computeHash(inPluginDirLocalFile) != plugin.hash) {
          throw new InternalProcessingError("Hash of a plugin does't match.")
        }
        usedFiles.put(plugin.src, inPluginDirLocalFile)
      }

      PluginManager.loadDir(pluginDir)

      /* --- Download the files for the local file cache ---*/
      
      for (repliURI <- executionMessage.files) {

        //To avoid getting twice the same plugin with different path
        if (!usedFiles.containsKey(repliURI.src)) {

          val cache = repliURI.replica.cache
          val cacheHash = HashService.computeHash(cache).toString

          if (cacheHash != repliURI.hash) {
            throw new InternalProcessingError("Hash is incorrect for file " + repliURI.src.toString + " replicated at " + repliURI.replica.toString)
          }

          val local = if (repliURI.directory) {
            val local = Workspace.newDir("dirReplica")
            new TarInputStream(new FileInputStream(cache)).extractDirArchiveWithRelativePathAndClose(local) 
            local
          } else  cache

          usedFiles.put(repliURI.src, local)
        }
      }
      
      val jobsFile = executionMessage.jobs.file
      val jobsFileCache = jobsFile.cache

      if (HashService.computeHash(jobsFileCache) != executionMessage.jobs.hash) {
        throw new InternalProcessingError("Hash of the execution job does't match.");
      }

      val tis = new TarInputStream(new FileInputStream(jobsFileCache))     
      val allMoleJobs =  tis.applyAndClose( e => {SerializerService.deserializeReplaceFiles[IMoleJob](tis, usedFiles)})

      //val jobForRuntime = Activator.getSerialiser.deserializeReplaceFiles[JobForRuntime](jobForRuntimeFileCache, usedFiles)
      jobsFileCache.delete
            
      try {

        /* --- Submit all jobs to the local environment --*/

        val allFinished = new AllFinished
        val saver = new ContextSaver

        for (toProcess <- allMoleJobs) {
          EventDispatcher.registerForObjectChangedSynchronous(toProcess, Priority.HIGH, saver, IMoleJob.StateChanged)
          allFinished.registerJob(toProcess)
          LocalExecutionEnvironment.default.submit(toProcess)
        }

        allFinished.waitAllFinished

        val contextResults = new ContextResults(saver.results)
        val contextResultFile = Workspace.newFile

        val serializationResult = SerializerService.serializeGetPluginClassAndFiles(contextResults, contextResultFile)

        val uploadedcontextResults = new GZURIFile(executionMessage.communicationDir.newFileInDir("uplodedTar", ".tgz"))

        retry( URIFile.copy(contextResultFile, uploadedcontextResults), NbRetry )

        contextResult = uploadedcontextResults
        contextResultFile.delete

        /*-- Tar the result files --*/

        val tarResult = Workspace.newFile("result", ".tar")

      
        if (!serializationResult.files.isEmpty) {
          val tos = new TarOutputStream(new FileOutputStream(tarResult))     
          try {
            for (file <- serializationResult.files) {
              //Logger.getLogger(classOf[Runtime].getName).info("Output file: " + file.getAbsolutePath)

              val name = UUID.randomUUID        
              val entry = new TarEntry(name.toString)

              val toArchive =  if (file.isDirectory) {
                val toArchive = Workspace.newFile
                val outputStream = new TarOutputStream(new FileOutputStream(toArchive))

                try outputStream.createDirArchiveWithRelativePath(file)
                finally outputStream.close
                
                toArchive
              } else file

              //TarArchiveEntry entry = new TarArchiveEntry(file.getName());
              entry.setSize(toArchive.length)
              tos.putNextEntry(entry)
              
              try toArchive.copy(tos) finally tos.closeEntry
              
              filesInfo.put(entry.getName, (file, file.isDirectory))
            }
          } finally tos.close

          val uploadedTar = new GZURIFile(executionMessage.communicationDir.newFileInDir("uplodedTar", ".tgz"))

          /*-- Try 3 times to write the result --*/

          retry(URIFile.copy(tarResult, uploadedTar), NbRetry)

          tarResultMessage = new FileMessage(uploadedTar, HashService.computeHash(tarResult).toString)
        } else {
          tarResultMessage = FileMessage.EMPTY_RESULT
        }
                
        tarResult.delete

      } finally {
        outSt.close
        errSt.close

        System.setOut(oldOut)
        System.setErr(oldErr)

        outputMessage = if (out.length != 0) {
          val output = new GZURIFile(executionMessage.communicationDir.newFileInDir("output", ".txt"))
          URIFile.copy(out, output)
          new FileMessage(output, HashService.computeHash(out).toString)
        } else FileMessage.EMPTY_RESULT

        errorMessage = if (err.length != 0) {                    
          val errout = new GZURIFile(executionMessage.communicationDir.newFileInDir("outputError", ".txt"))
          URIFile.copy(err, errout)
          new FileMessage(errout, HashService.computeHash(err).toString)
        } else FileMessage.EMPTY_RESULT
        
        out.delete
        err.delete
      }
    } catch {
      case(t: Throwable) => {
          if(debug) Logger.getLogger(classOf[Runtime].getName).log(Level.SEVERE, "", t)
          exception = t
        }
    }

    val runtimeResult = new RuntimeResult(outputMessage, errorMessage, tarResultMessage, exception, filesInfo, contextResult)
        
    val outputLocal = Workspace.newFile("output", ".res")
    SerializerService.serialize(runtimeResult, outputLocal)
    try {
      val output = new GZURIFile(new URIFile(resultMessageURI))
      retry(URIFile.copy(outputLocal, output), NbRetry)
    } finally outputLocal.delete
  }
  
}
