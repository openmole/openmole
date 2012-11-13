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

import akka.actor.Actor
import akka.actor.ActorRef
import java.io.FileInputStream
import java.io.IOException
import org.openmole.core.batch.control._
import org.openmole.core.batch.environment._
import org.openmole.core.batch.message._
import org.openmole.core.batch.storage._
import org.openmole.core.model.execution._
import org.openmole.core.model.job._
import org.openmole.core.serializer.SerializerService
import org.openmole.core.batch.environment.BatchEnvironment._
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.hashservice.HashService
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.workspace._

object GetResultActor extends Logger

class GetResultActor(jobManager: ActorRef) extends Actor {

  def receive = {
    case GetResult(job, sj, resultPath) ⇒
      try getResult(sj.storage, resultPath, job)
      catch {
        case e: Throwable ⇒ jobManager ! Error(job, e)
      }
      jobManager ! Kill(job)
      System.runFinalization
  }

  def getResult(storage: StorageService, outputFilePath: String, batchJob: BatchExecutionJob): Unit = {
    import batchJob.job




    storage.withToken { implicit token ⇒
      val runtimeResult = getRuntimeResult(outputFilePath, storage)

      display(runtimeResult.stdOut, "Output", storage)
      display(runtimeResult.stdErr, "Error output", storage)

      runtimeResult.result match {
        case Right(exception) ⇒ throw new JobRemoteExecutionException(exception, "Fatal exception thrown durring the execution of the job execution on the excution node")
        case Left(result) ⇒
          val contextResults = getContextResults(result, storage)

          var firstRunning = Long.MaxValue
          var lastCompleted = 0L

          //Try to download the results for all the jobs of the group
          for (moleJob ← job.moleJobs) {
            if (contextResults.results.isDefinedAt(moleJob.id)) {
              val executionResult = contextResults.results(moleJob.id)

              moleJob.synchronized {
                if (!moleJob.finished) {

                  executionResult._1 match {
                    case Left(context) ⇒
                      /*val timeStamps = executionResult._2
                      val completed = timeStamps.view.reverse.find(_.state == State.COMPLETED).get.time
                      if (completed > lastCompleted) lastCompleted = completed
                      val running = timeStamps.view.reverse.find(_.state == State.RUNNING).get.time
                      if (running < firstRunning) firstRunning = running*/
                      moleJob.finish(context, executionResult._2)
                    case Right(e) ⇒
                      sender ! MoleJobError(moleJob, batchJob, e)
                  }
                }
              }
            }
          }

          batchJob.environment.statistics += new StatisticSample(batchJob.batchJob.get)

        /*if(firstRunning != Long.MaxValue && lastCompleted != 0L) 
           environment.statistics += new StatisticSample(batchJob.batchJob.get.timeStamp(ExecutionState.SUBMITTED), firstRunning, lastCompleted)*/

      }
    }
  }

  private def getRuntimeResult(outputFilePath: String, storage: StorageService)(implicit token: AccessToken): RuntimeResult = {
    val resultFile = Workspace.newFile
    try {
      signalDownload(storage.downloadGZ(outputFilePath, resultFile), outputFilePath, storage)
      SerializerService.deserialize(resultFile)
    } finally resultFile.delete
  }

  private def display(message: Option[FileMessage], description: String, storage: StorageService)(implicit token: AccessToken) = {
    message match {
      case Some(message) ⇒
        try {
          val tmpFile = Workspace.newFile
          try {
            signalDownload(storage.downloadGZ(message.path, tmpFile), message.path, storage)

            /*val stdOutHash = HashService.computeHash(stdOutFile)
             if (stdOutHash != message.hash)
             logger.log(WARNING, "The standard output has been corrupted durring the transfert.")
             */

            System.out.synchronized {
              System.out.println("-----------------" + description + " on remote host-----------------")
              val fis = new FileInputStream(tmpFile)
              try fis.copy(System.out) finally fis.close
              System.out.println("-------------------------------------------------------")
            }
          } finally tmpFile.delete
        } catch {
          case (e: IOException) ⇒
            GetResultActor.logger.log(WARNING, description + " transfer has failed.")
            GetResultActor.logger.log(FINE, "Stack of the error durring tranfert", e)
        }
      case None ⇒
    }
  }

  private def getContextResults(resultPath: FileMessage, storage: StorageService)(implicit token: AccessToken): ContextResults = {
    if (resultPath == null) throw new InternalProcessingError("Context results path is null")
    val contextResutsFileCache = Workspace.newFile
    try {
      signalDownload(storage.downloadGZ(resultPath.path, contextResutsFileCache), resultPath.path, storage)
      if (HashService.computeHash(contextResutsFileCache) != resultPath.hash) throw new InternalProcessingError("Results have been corrupted durring the transfer.")
      SerializerService.deserializeAndExtractFiles(contextResutsFileCache)
    } finally contextResutsFileCache.delete
  }

}
