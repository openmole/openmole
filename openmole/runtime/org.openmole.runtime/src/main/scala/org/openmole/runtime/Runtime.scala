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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.runtime

import java.io.File
import java.io.PrintStream
import java.util.TreeMap
import java.util.TreeMap
import java.util.concurrent.Callable
import java.util.logging.Level
import java.util.logging.Logger
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.commons.tools.io.FileInputStream
import org.openmole.commons.tools.io.FileOutputStream
import org.openmole.commons.tools.io.FileUtil
import org.openmole.commons.tools.io.StringInputStream
import org.openmole.commons.tools.io.TarArchiver
import org.openmole.commons.tools.service.Priority
import org.openmole.core.file.GZURIFile
import org.openmole.core.file.URIFile
import org.openmole.core.implementation.execution.local.LocalExecutionEnvironment
import org.openmole.core.implementation.message.ContextResults
import org.openmole.core.implementation.message.ExecutionMessage
import org.openmole.core.implementation.message.FileMessage
import org.openmole.core.implementation.message.JobForRuntime
import org.openmole.core.implementation.message.RuntimeResult
import org.openmole.core.model.file.IURIFile
import org.openmole.core.model.job.IMoleJob
import org.openmole.runtime.internal.Activator
import org.openmole.commons.tools.service.Retry._

import scala.collection.JavaConversions._

object Runtime {
  val NumberOfLocalTheads = 1
  val NbRetry = 3
}

class Runtime {

  import Runtime._
  
  
  
  
  def apply(executionMessageURI: String, resultMessageURI: String, debug: Boolean) = {
    
 
    val oldOut = System.out
    val oldErr = System.err

    val out = Activator.getWorkspace.newFile("openmole", ".out")
    val err = Activator.getWorkspace.newFile("openmole", ".err")

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
        
    val filesInfo = new TreeMap[String, (File, Boolean)]
    var contextResult: IURIFile = null
        
    try {
      Activator.getWorkspace.setPreference(LocalExecutionEnvironment.DefaultNumberOfThreads, Integer.toString(NumberOfLocalTheads));
                        
      /*--- get execution message and job for runtime---*/
      val usedFiles = new TreeMap[File, File]
            
      val executionMessageFile = new GZURIFile(new URIFile(executionMessageURI));
      val executionMesageFileCache = executionMessageFile.cache
      val executionMessage = Activator.getSerialiser.deserialize[ExecutionMessage](executionMesageFileCache);
      executionMesageFileCache.delete
            
      val pluginDir = Activator.getWorkspace.newDir

      for (plugin <- executionMessage.plugins) {
        val replicaFileCache = plugin.replica.cache

        val inPluginDirLocalFile = File.createTempFile("plugin", ".jar", pluginDir)
        replicaFileCache.renameTo(inPluginDirLocalFile)

        if (!Activator.getHashService.computeHash(inPluginDirLocalFile).equals(plugin.hash)) {
          throw new InternalProcessingError("Hash of a plugin does't match.")
        }

        usedFiles.put(plugin.src, inPluginDirLocalFile)
      }


      Activator.getPluginManager.loadDir(pluginDir);

      /* --- Download the files for the local file cache ---*/
      
      for (repliURI <- executionMessage.files) {

        //To avoid getting twice the same plugin with different path
        if (!usedFiles.containsKey(repliURI.src)) {

          val cache = repliURI.replica.cache

          val cacheHash = Activator.getHashService.computeHash(cache)

          if (!cacheHash.equals(repliURI.hash)) {
            throw new InternalProcessingError("Hash is incorrect for file " + repliURI.src.toString + " replicated at " + repliURI.replica.toString)
          }

          val local = if (repliURI.directory) {
            val local = Activator.getWorkspace.newDir("dirReplica")
            val is = new FileInputStream(cache)

            try {
              TarArchiver.extractDirArchiveWithRelativePath(local, is)
            } finally {
              is.close
            }
            local
          } else {
            cache;
          }

          usedFiles.put(repliURI.src, local)
        }
      }

            
      val jobForRuntimeFile = executionMessage.jobForRuntime.file
      val jobForRuntimeFileCache = jobForRuntimeFile.cache

      if (!Activator.getHashService.computeHash(jobForRuntimeFileCache).equals(executionMessage.jobForRuntime.hash)) {
        throw new InternalProcessingError("Hash of the execution job does't match.");
      }

      val jobForRuntime = Activator.getSerialiser.deserializeReplaceFiles[JobForRuntime](jobForRuntimeFileCache, usedFiles)
      jobForRuntimeFileCache.delete
            
      try {

        /* --- Submit all jobs to the local environment --*/

        val allFinished = new AllFinished
        val saver = new ContextSaver

        for (toProcess <- jobForRuntime.moleJobs) {
          Activator.getEventDispatcher.registerForObjectChangedSynchronous(toProcess, Priority.HIGH, saver, IMoleJob.StateChanged)
          allFinished.registerJob(toProcess)
          LocalExecutionEnvironment.submit(toProcess)
        }

        allFinished.waitAllFinished

        val contextResults = new ContextResults(saver.results)
        val contextResultFile = Activator.getWorkspace.newFile

        val serializationResult = Activator.getSerialiser.serializeGetPluginClassAndFiles(contextResults, contextResultFile)

        val uploadedcontextResults = new GZURIFile(executionMessage.communicationDir.newFileInDir("uplodedTar", ".tgz"))

        retry( URIFile.copy(contextResultFile, uploadedcontextResults), NbRetry )

        contextResult = uploadedcontextResults
        contextResultFile.delete

        /*-- Tar the result files --*/

        val tarResult = Activator.getWorkspace.newFile("result", ".tar")

        val tos = new TarArchiveOutputStream(new FileOutputStream(tarResult))
        tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)

                
        if (!serializationResult._1.isEmpty) {
                    
          try {
            for (file <- serializationResult._1) {
              val is = new StringInputStream(file.getCanonicalPath)

              val hash = try {
                Activator.getHashService.computeHash(is);
              } finally {
                is.close
              }
                            
              val entry = new TarArchiveEntry(hash.toString())

              val toArchive =  if (file.isDirectory) {
                val toArchive = Activator.getWorkspace.newFile
                val outputStream = new FileOutputStream(toArchive)

                try {
                  TarArchiver.createDirArchiveWithRelativePath(file, outputStream)
                } finally {
                  outputStream.close
                }
                toArchive
              } else {
                file
              }

              //TarArchiveEntry entry = new TarArchiveEntry(file.getName());
              entry.setSize(toArchive.length)
              tos.putArchiveEntry(entry)
              try {
                FileUtil.copy(new FileInputStream(toArchive), tos);
              } finally {
                tos.closeArchiveEntry
              }

              filesInfo.put(entry.getName, (file, file.isDirectory))
            }
          } finally {
            tos.close
          }

          val uploadedTar = new GZURIFile(executionMessage.communicationDir.newFileInDir("uplodedTar", ".tgz"))

          /*-- Try 3 times to write the result --*/

          retry(URIFile.copy(tarResult, uploadedTar), NbRetry)

          tarResultMessage = new FileMessage(uploadedTar, Activator.getHashService.computeHash(tarResult));
        } else {
          tarResultMessage = FileMessage.EMPTY_RESULT
        }
                
        tarResult.delete

      } finally {
        outSt.close
        errSt.close

        System.setOut(oldOut)
        System.setErr(oldErr)


        outputMessage = if (out.length() != 0) {
          val output = new GZURIFile(executionMessage.communicationDir.newFileInDir("output", ".txt"))
          URIFile.copy(out, output)
          new FileMessage(output, Activator.getHashService().computeHash(out))
        } else {
          FileMessage.EMPTY_RESULT
        }

        errorMessage = if (err.length != 0) {                    
          val errout = new GZURIFile(executionMessage.communicationDir.newFileInDir("outputError", ".txt"))
          URIFile.copy(err, errout)
          new FileMessage(errout, Activator.getHashService().computeHash(err))
        } else {
          FileMessage.EMPTY_RESULT
        }

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
        
    val outputLocal = Activator.getWorkspace.newFile("output", ".res")
    Activator.getSerialiser.serialize(runtimeResult, outputLocal)
    try {
      val output = new GZURIFile(new URIFile(resultMessageURI))

      retry(URIFile.copy(outputLocal, output), NbRetry)

    } finally {
      outputLocal.delete
    }

  }
  
}
