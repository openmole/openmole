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

package org.openmole.core.batch.environment

import com.ice.tar.TarInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
//import java.util.TreeMap
//import java.util.concurrent.Callable
import java.util.concurrent.Callable
import java.util.logging.Level
import java.util.logging.Logger
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.tools.io.TarArchiver._
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.message.ContextResults
import org.openmole.core.batch.message.FileMessage
import org.openmole.core.batch.message.RuntimeResult
import org.openmole.core.batch.control.BatchStorageControl
import org.openmole.core.batch.control.BatchStorageDescription
import org.openmole.core.batch.file.IURIFile
import org.openmole.core.model.job.IJob
import org.openmole.core.implementation.execution.StatisticSample
import org.openmole.core.implementation.task.GenericTask
import org.openmole.core.model.execution.ExecutionState._

//import scala.collection.JavaConversions._

import org.openmole.core.model.job.State
import org.openmole.core.serializer.SerializerService
import org.openmole.misc.hashservice.HashService
import org.openmole.misc.workspace.Workspace
import scala.Boolean._
import scala.collection.immutable.TreeMap

object GetResultFromEnvironment {
  private val LOGGER = Logger.getLogger(GetResultFromEnvironment.getClass.getName)
}

class GetResultFromEnvironment(communicationStorageDescription: BatchStorageDescription, outputFile: IURIFile, job: IJob, environment: BatchEnvironment, batchJob: BatchJob) extends Callable[Unit] {
  import GetResultFromEnvironment._

  private def successFullFinish(running: Long, done: Long) = {
    import batchJob.timeStemp
    environment.sample(job, new StatisticSample(timeStemp(SUBMITTED), running, done))
  }

  override def call: Unit = {
    val token = BatchStorageControl.usageControl(communicationStorageDescription).waitAToken

    try {
      val result = getRuntimeResult(outputFile, token)

      if (result.exception != null) {
        throw new InternalProcessingError(result.exception, "Fatal exception thrown durring the execution of the job execution on the excution node")
      }

      display(result.stdOut, "Output", token)
      display(result.stdErr, "Error output", token)

      val fileReplacement = getFiles(result.tarResult, result.filesInfo, token)

      val contextResults = getContextResults(result.contextResultURI, fileReplacement, token)

      var successfull = 0
      var firstRunning = Long.MaxValue
      var lastCompleted = 0L
      
      //Try to download the results for all the jobs of the group
      for (moleJob <- job.moleJobs) {
        if (contextResults.results.isDefinedAt(moleJob.id)) {
          val context = contextResults.results(moleJob.id)
 
          moleJob.synchronized {
            if (!moleJob.isFinished) {
              try {
                moleJob.rethrowException(context)
                
                context.value(GenericTask.Timestamps.prototype) match {
                  case Some(stamps) => 
                    val completed = stamps.view.reverse.find( _.state == State.COMPLETED ).get.time
                    if(completed > lastCompleted) lastCompleted = completed
                    val running = stamps.view.reverse.find( _.state == State.RUNNING ).get.time
                    if(running < firstRunning) firstRunning = running
                  case None => LOGGER.log(Level.WARNING, "No time stamps found.")
                }
                
                moleJob.finished(context)
                successfull +=1 
              } catch {
                case e => LOGGER.log(Level.WARNING, "Error durring job execution, it will be resubmitted.", e)
              }
            }
          }
        }
      }

      //If sucessfull for full group update stats
      if (successfull == job.moleJobs.size) successFullFinish(firstRunning, lastCompleted)

    } finally {
      BatchStorageControl.usageControl(communicationStorageDescription).releaseToken(token)
    }
  }


  private def getRuntimeResult(outputFile: IURIFile, token: AccessToken): RuntimeResult = {
    val resultFile = outputFile.cache(token)
    try {
      SerializerService.deserialize(resultFile)
    } finally {
      resultFile.delete
    }
  }

  private def display(message: FileMessage, description: String, token: AccessToken) = {
    if (message == null) {
      LOGGER.log(Level.WARNING, "{0} is null.", description)
    } else {
      try {
        if (!message.isEmpty) {
          val stdOutFile = message.file.cache(token)
          try {
            val stdOutHash = HashService.computeHash(stdOutFile)
            if (!stdOutHash.equals(message.hash)) {
              LOGGER.log(Level.WARNING, "The standard output has been corrupted durring the transfert.")
            }

            System.out.synchronized {
              System.out.println("-----------------" + description + " on remote host-----------------")
              val fis = new FileInputStream(stdOutFile)
              try fis.copy(System.out) finally fis.close
              System.out.println("-------------------------------------------------------")
            }
          } finally {
            stdOutFile.delete
          }
        }
      } catch {
        case(e: IOException) => LOGGER.log(Level.WARNING, description + " transfer has failed.", e);
      }
    }
  }

  private def getFiles(tarResult: FileMessage, filesInfo: PartialFunction[String, (File, Boolean)], token: AccessToken): Map[File, File] = {
    if (tarResult == null) throw new InternalProcessingError("TarResult uri result is null.")

    var fileReplacement = new TreeMap[File, File]

    if (!tarResult.isEmpty) {
      val tarResultURIFile = tarResult.file
      val tarResultFile = tarResultURIFile.cache(token)

      try {
        val tarResulHash = HashService.computeHash(tarResultFile)
        if (!tarResulHash.equals(tarResult.hash)) {
          throw new InternalProcessingError("Archive has been corrupted durring transfert from the execution environment.")
        }

        val tis = new TarInputStream(new FileInputStream(tarResultFile))

        tis.applyAndClose(te => {
            val dest = Workspace.newFile("result", ".bin")//new File(workspace.tmpDir, )
             
            val os = new FileOutputStream(dest)

            try tis.copy(os) finally os.close

            val fileInfo = filesInfo(te.getName)
            if (fileInfo == null) throw new InternalProcessingError("FileInfo not found for entry " + te.getName + '.')

            val file = if (fileInfo._2) {
              val file = Workspace.newDir("tarResult")

              new TarInputStream(new FileInputStream(dest)).extractDirArchiveWithRelativePathAndClose(file)
              dest.delete
              file
            } else dest
            fileReplacement += fileInfo._1 -> file
          })
      } finally {
        tarResultFile.delete
      }
    }
    fileReplacement
  }

  private def getContextResults(uriFile: IURIFile, fileReplacement: PartialFunction[File, File], token: AccessToken): ContextResults = {
    if (uriFile == null) throw new InternalProcessingError("Context results URI is null")
    val contextResutsFileCache = uriFile.cache(token)

    try SerializerService.deserializeReplaceFiles(contextResutsFileCache, fileReplacement)
    finally contextResutsFileCache.delete
  }
}
