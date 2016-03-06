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

package org.openmole.core.batch.refresh

import java.io.{ PrintStream, FileInputStream, IOException }
import org.openmole.core.batch.control._
import org.openmole.core.batch.environment._
import org.openmole.core.batch.message._
import org.openmole.core.batch.storage._
import org.openmole.core.event.EventDispatcher
import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.serializer.structure.PluginClassAndFiles
import org.openmole.core.workflow.execution.Environment.RuntimeLog
import org.openmole.tool.file._
import org.openmole.tool.hash._
import org.openmole.core.workflow.execution._
import org.openmole.core.workflow.execution
import org.openmole.core.workflow.job._
import org.openmole.core.serializer.SerialiserService
import org.openmole.core.batch.environment.BatchEnvironment._
import org.openmole.core.workspace.Workspace
import org.openmole.tool.logger.Logger
import util.{ Failure, Success }

object GetResultActor extends Logger

import GetResultActor.Log._

class GetResultActor(jobManager: JobManager) {

  def receive(msg: GetResult) = withRunFinalization {
    val GetResult(job, sj, resultPath) = msg
    try sj.storage.tryWithToken {
      case Some(token) ⇒
        getResult(sj.storage, resultPath, job)(token)
        jobManager ! Kill(job)
      case None ⇒
        jobManager ! Delay(msg, getTokenInterval)
    } catch {
      case e: Throwable ⇒
        job.state = ExecutionState.FAILED
        jobManager ! Error(job, e)
        jobManager ! Kill(job)
    }
  }

  def getResult(storage: StorageService, outputFilePath: String, batchJob: BatchExecutionJob)(implicit token: AccessToken): Unit = {
    import batchJob.job
    val runtimeResult = getRuntimeResult(outputFilePath, storage)

    val stream = job.moleExecution.executionContext.out
    display(runtimeResult.stdOut, s"Output on ${runtimeResult.info.hostName}", storage, stream)
    display(runtimeResult.stdErr, s"Error output ${runtimeResult.info.hostName}", storage, stream)

    runtimeResult.result match {
      case Failure(exception) ⇒ throw new JobRemoteExecutionException(exception, "Fatal exception thrown during the execution of the job execution on the execution node")
      case Success((result, log)) ⇒
        val contextResults = getContextResults(result, storage)

        EventDispatcher.trigger(storage.environment: Environment, Environment.JobCompleted(batchJob, log, runtimeResult.info))

        //Try to download the results for all the jobs of the group
        for (moleJob ← job.moleJobs) {
          if (contextResults.results.isDefinedAt(moleJob.id)) {
            val executionResult = contextResults.results(moleJob.id)
            executionResult match {
              case Success(context) ⇒ moleJob.finish(context)
              case Failure(e)       ⇒ if (!moleJob.finished) jobManager ! MoleJobError(moleJob, batchJob, e)
            }
          }
        }
    }
  }

  private def getRuntimeResult(outputFilePath: String, storage: StorageService)(implicit token: AccessToken): RuntimeResult = Workspace.withTmpFile { resultFile ⇒
    signalDownload(storage.download(outputFilePath, resultFile), outputFilePath, storage, resultFile)
    SerialiserService.deserialiseAndExtractFiles[RuntimeResult](resultFile)
  }

  private def display(output: Option[File], description: String, storage: StorageService, stream: PrintStream)(implicit token: AccessToken) = {
    output.foreach { file ⇒
      execution.display(stream, description, file.content)
      file.delete()
    }
  }

  private def getContextResults(serializedResults: SerializedContextResults, storage: StorageService)(implicit token: AccessToken): ContextResults = {
    serializedResults match {
      case serializedResults: IndividualFilesContextResults ⇒
        Workspace.withTmpFile { serializedResultsFile ⇒
          val fileReplacement =
            serializedResults.files.map {
              replicated ⇒
                replicated.originalPath → replicated.download((p, f) ⇒ signalDownload(storage.download(p, f, TransferOptions(forceCopy = true, canMove = true)), p, storage, f))
            }.toMap

          val res = SerialiserService.deserialiseReplaceFiles[ContextResults](serializedResults.contextResults, fileReplacement)
          serializedResults.contextResults.delete()
          res
        }
      case serializedResults: ArchiveContextResults ⇒
        val res = SerialiserService.deserialiseAndExtractFiles[ContextResults](serializedResults.contextResults)
        serializedResults.contextResults.delete()
        res
    }
  }

}
