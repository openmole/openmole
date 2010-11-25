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

package org.openmole.core.implementation.execution.batch

import java.io.File
import java.io.IOException
import java.util.TreeMap
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.logging.Level
import java.util.logging.Logger
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.commons.exception.UserBadDataError
import org.openmole.commons.tools.io.FileInputStream
import org.openmole.commons.tools.io.FileOutputStream
import org.openmole.commons.tools.io.FileUtil
import org.openmole.commons.tools.io.TarArchiver
import org.openmole.core.implementation.message.ContextResults
import org.openmole.core.implementation.message.FileMessage
import org.openmole.core.implementation.message.RuntimeResult
import org.openmole.core.implementation.internal.Activator
import org.openmole.core.model.execution.batch.BatchServiceDescription
import org.openmole.core.model.execution.batch.IAccessToken
import org.openmole.core.model.file.IURIFile
import org.openmole.core.model.job.IJob
import org.openmole.core.model.execution.SampleType

import scala.collection.JavaConversions._
import scala.Boolean._

object GetResultFromEnvironment {
  private val LOGGER = Logger.getLogger(GetResultFromEnvironment.getClass.getName)
}

class GetResultFromEnvironment(communicationStorageDescription: BatchServiceDescription, outputFile: IURIFile, job: IJob, environment: BatchEnvironment, lastStatusChangeInterval: Long) extends Callable[Unit] {
  import GetResultFromEnvironment._

  private def successFullFinish = {
    environment.sample(SampleType.RUNNING, lastStatusChangeInterval, job)
  }

  override def call: Unit = {
    val token = Activator.getBatchRessourceControl.usageControl(communicationStorageDescription).waitAToken

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

      //Try to download the results for all the jobs of the group
      for (moleJob <- job.moleJobs) {
        if (contextResults.results.isDefinedAt(moleJob.id)) {
          val context = contextResults.results(moleJob.id)
 
          moleJob.synchronized {
            if (!moleJob.isFinished) {
              try {
                moleJob.rethrowException(context)
                moleJob.finished(context)
                successfull +=1 
              } catch {
                case e => LOGGER.log(Level.WARNING, "Error durring job execution, it will be resubmitted.", e);
              }
            }
          }
        }
      }

      //If sucessfull for full group update stats
      if (successfull == job.moleJobs.size) {
        successFullFinish
      }

    } finally {
      Activator.getBatchRessourceControl.usageControl(communicationStorageDescription).releaseToken(token)
    }
  }


  private def getRuntimeResult(outputFile: IURIFile, token: IAccessToken): RuntimeResult = {
    val resultFile = outputFile.cache(token)
    try {
      return Activator.getSerializer.deserialize(resultFile)
    } finally {
      resultFile.delete
    }
  }

  private def display(message: FileMessage, description: String, token: IAccessToken) = {
    if (message == null) {
      LOGGER.log(Level.WARNING, "{0} is null.", description)
    } else {
      try {
        if (!message.isEmpty) {
          val stdOutFile = message.file.cache(token)
          try {
            val stdOutHash = Activator.getHashService.computeHash(stdOutFile)
            if (!stdOutHash.equals(message.hash)) {
              LOGGER.log(Level.WARNING, "The standard output has been corrupted durring the transfert.")
            }

            System.out.synchronized {
              System.out.println("-----------------" + description + " on remote host-----------------")
              val fis = new FileInputStream(stdOutFile)
              try {
                FileUtil.copy(fis, System.out);
              } finally {
                fis.close
              }
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

  private def getFiles(tarResult: FileMessage, filesInfo: PartialFunction[String, (File, Boolean)], token: IAccessToken): Map[File, File] = {
    if (tarResult == null) {
      throw new InternalProcessingError("TarResult uri result is null.")
    }

    val fileReplacement = new TreeMap[File, File]

    if (!tarResult.isEmpty) {
      val tarResultURIFile = tarResult.file
      val tarResultFile = tarResultURIFile.cache(token)

      try {
        val tarResulHash = Activator.getHashService().computeHash(tarResultFile)
        if (!tarResulHash.equals(tarResult.hash)) {
          throw new InternalProcessingError("Archive has been corrupted durring transfert from the execution environment.")
        }

        val tis = new TarArchiveInputStream(new FileInputStream(tarResultFile));

        try {
          val destDir = Activator.getWorkspace.newDir("tarResult")

          var te: ArchiveEntry = tis.getNextEntry

          while (te != null) {
            val dest = new File(destDir, te.getName)
            val os = new FileOutputStream(dest)

            try {
              FileUtil.copy(tis, os);
            } finally {
              os.close
            }

            val fileInfo = filesInfo(te.getName)
            if (fileInfo == null) {
              throw new InternalProcessingError("Filename not found for entry " + te.getName + '.')
            }

            val file = if (fileInfo._2) {
              val file = Activator.getWorkspace().newDir("tarResult")

              val destIn = new FileInputStream(dest)
              try {
                TarArchiver.extractDirArchiveWithRelativePath(file, destIn);
              } finally {
                destIn.close
              }
              file
            } else {
              dest
            }
            fileReplacement(fileInfo._1) = file
            te = tis.getNextEntry
          }

        } finally {
          tis.close
        }
      } finally {
        tarResultFile.delete
      }
    }
    fileReplacement.toMap
  }

  private def getContextResults(uriFile: IURIFile, fileReplacement: PartialFunction[File, File], token: IAccessToken): ContextResults = {

    //Download and deserialize the context results

    if (uriFile == null) {
      throw new InternalProcessingError("Context results URI is null")
    }

    val contextResutsFileCache = uriFile.cache(token)

    try {
      return Activator.getSerializer.deserializeReplaceFiles(contextResutsFileCache, fileReplacement);
    } finally {
      contextResutsFileCache.delete
    }
  }
}
