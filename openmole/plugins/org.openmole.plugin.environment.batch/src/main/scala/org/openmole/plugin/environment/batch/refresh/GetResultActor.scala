/*
 * Copyright (C) 2012 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

package org.openmole.plugin.environment.batch.refresh

import java.io.PrintStream
import org.openmole.core.communication.message._
import org.openmole.core.communication.storage._
import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.outputmanager.OutputManager
import org.openmole.core.tools.service.Retry._
import org.openmole.core.workflow.execution
import org.openmole.core.workflow.execution._
import org.openmole.plugin.environment.batch.environment.BatchEnvironment._
import org.openmole.plugin.environment.batch.environment._
import org.openmole.tool.file._
import org.openmole.tool.logger.JavaLogger
import org.openmole.core.workflow.job._

import scala.util.{ Failure, Success }

object GetResultActor {

  case class JobRemoteExecutionException(message: String, cause: Throwable) extends InternalProcessingError(message, cause)

  def receive(msg: GetResult)(implicit services: BatchEnvironment.Services) = {
    import services._

    val GetResult(job, resultPath, batchJob) = msg

    JobManager.killOr(job, Kill(job, Some(batchJob))) { () ⇒
      try getResult(batchJob.storageId(), batchJob.environment, batchJob.download, resultPath, job)
      catch {
        case e: Throwable ⇒
          job.state = ExecutionState.FAILED
          val stdOutErr = BatchJobControl.tryStdOutErr(batchJob).toOption
          JobManager ! Error(job, e, stdOutErr)
      }
      finally {
        JobManager ! Kill(job, Some(batchJob))
      }
    }
  }

  def getResult(storageId: String, environment: BatchEnvironment, download: (String, File, TransferOptions) ⇒ Unit, outputFilePath: String, batchJob: BatchExecutionJob)(implicit services: BatchEnvironment.Services): Unit = {
    import batchJob.job

    val runtimeResult = getRuntimeResult(outputFilePath, storageId, environment, download)

    val stream = batchJob.storedJob.moleExecution.executionContext.services.outputRedirection.output
    display(runtimeResult.stdOut, s"Output on ${runtimeResult.info.hostName}", stream)

    runtimeResult.result match {
      case Failure(exception) ⇒ throw new JobRemoteExecutionException("Fatal exception thrown during the execution of the job execution on the execution node", exception)
      case Success((serializedContextResults, log)) ⇒
        val contextResults = getContextResults(serializedContextResults, storageId, environment, download)

        services.eventDispatcher.trigger(environment: Environment, Environment.JobCompleted(batchJob, log, runtimeResult.info))

        //Try to download the results for all the jobs of the group
        for (moleJob ← batchJob.storedJob.storedMoleJobs) {
          if (contextResults.results.isDefinedAt(moleJob.id)) {
            val executionResult = contextResults.results(moleJob.id)
            executionResult match {
              case Success(context) ⇒ JobStore.finish(moleJob, Left(context))
              case Failure(e)       ⇒ JobManager ! MoleJobError(moleJob.id, batchJob, e)
            }
          }
        }
    }
  }

  private def getRuntimeResult(outputFilePath: String, storageId: String, environment: BatchEnvironment, download: (String, File, TransferOptions) ⇒ Unit)(implicit services: BatchEnvironment.Services): RuntimeResult = {
    import services._
    retry(preference(BatchEnvironment.downloadResultRetry)) {
      newFile.withTmpFile { resultFile ⇒
        signalDownload(eventDispatcher.eventId, download(outputFilePath, resultFile, TransferOptions.default), outputFilePath, environment, storageId, resultFile)
        serializerService.deserializeAndExtractFiles[RuntimeResult](resultFile, deleteFilesOnGC = true)
      }
    }
  }

  private def display(output: Option[File], description: String, stream: PrintStream) = {
    output.foreach { file ⇒
      execution.display(stream, description, file.content)
      file.delete()
    }
  }

  private def getContextResults(serializedResults: SerializedContextResults, storageId: String, environment: BatchEnvironment, download: (String, File, TransferOptions) ⇒ Unit)(implicit services: BatchEnvironment.Services): ContextResults = {
    import services._
    serializedResults match {
      case serializedResults: IndividualFilesContextResults ⇒
        val fileReplacement =
          serializedResults.files.map {
            replicated ⇒
              val downloaded =
                ReplicatedFile.download(replicated) { (p, f) ⇒
                  retry(preference(BatchEnvironment.downloadResultRetry)) {
                    signalDownload(eventDispatcher.eventId, download(p, f, TransferOptions(noLink = true, canMove = true)), p, environment, storageId, f)
                  }
                }

              replicated.originalPath → fileService.wrapRemoveOnGC(downloaded)
          }.toMap

        serializerService.deserializeReplaceFiles[ContextResults](serializedResults.contextResults, fileReplacement)
      case serializedResults: ArchiveContextResults ⇒
        serializerService.deserializeAndExtractFiles[ContextResults](serializedResults.contextResults, deleteFilesOnGC = true)
    }
  }

}
