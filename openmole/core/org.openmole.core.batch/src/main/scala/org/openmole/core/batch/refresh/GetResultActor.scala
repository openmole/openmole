/*
 * Copyright (C) 2012 reuillon
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
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.control.UsageControl
import org.openmole.core.batch.environment.BatchExecutionJob
import org.openmole.core.batch.environment.Storage
import org.openmole.core.batch.message.ContextResults
import org.openmole.core.batch.message.FileMessage
import org.openmole.core.batch.message.RuntimeResult
import org.openmole.core.model.job.IJob
import org.openmole.core.model.job.State
import org.openmole.core.serializer.SerializerService
import org.openmole.core.batch.environment.BatchEnvironment._
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.hashservice.HashService
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.tools.service.Logger


object GetResultActor extends Logger

class GetResultActor(jobManager: ActorRef) extends Actor {
  def receive = {
    case GetResult(job, sj, resultPath) => 
      //logger.fine("Getting results.")
      try getResult(sj.communicationStorage, resultPath, job)
      catch {
        case e => jobManager ! Error(job, e)
      }
      jobManager ! Kill(job)
  }
  
  def getResult(communicationStorage: Storage, outputFilePath: String, batchJob: BatchExecutionJob): Unit = {
    import communicationStorage._
    import batchJob.job
    
    val token = UsageControl.get(communicationStorage.description).waitAToken

    try {
      val runtimeResult = getRuntimeResult(outputFilePath, communicationStorage, token)
      
      display(runtimeResult.stdOut, "Output", communicationStorage, token)
      display(runtimeResult.stdErr, "Error output", communicationStorage, token)
      
      runtimeResult.result match {
        case Right(exception) => throw new JobRemoteExecutionException(exception, "Fatal exception thrown durring the execution of the job execution on the excution node")
        case Left(result) => 
          val contextResults = getContextResults(result, communicationStorage, token)

          //var successfull = 0
          var firstRunning = Long.MaxValue
          var lastCompleted = 0L

          //Try to download the results for all the jobs of the group
          for (moleJob <- job.moleJobs) {
            if (contextResults.results.isDefinedAt(moleJob.id)) {
              val executionResult = contextResults.results(moleJob.id)
         
              moleJob.synchronized {
                if (!moleJob.isFinished) {

                  executionResult._1 match {
                    case Left(context) =>
                      val timeStamps = executionResult._2
                      val completed = timeStamps.view.reverse.find( _.state == State.COMPLETED ).get.time
                      if(completed > lastCompleted) lastCompleted = completed
                      val running = timeStamps.view.reverse.find( _.state == State.RUNNING ).get.time
                      if(running < firstRunning) firstRunning = running
                      moleJob.finished(context, executionResult._2)
                    case Right(e) => 
                      sender ! MoleJobError(moleJob, batchJob, e)
                  }
                } 
              } 
            }
          }

      }
    } finally UsageControl.get(communicationStorage.description).releaseToken(token)
  }


  private def getRuntimeResult(outputFilePath: String, communicationStorage: Storage, token: AccessToken): RuntimeResult = {
    import communicationStorage.path
    val resultFile = signalDownload(path.cacheUnziped(outputFilePath, token), path.toURI(outputFilePath), communicationStorage)
    try SerializerService.deserialize(resultFile)
    finally resultFile.delete
  }

  private def display(message: Option[FileMessage], description: String, communicationStorage: Storage, token: AccessToken) = {
    import communicationStorage.path
    message match {
      case Some(message) =>
        try {
          val stdOutFile = signalDownload(path.cacheUnziped(message.path, token), path.toURI(message.path), communicationStorage)
          try {
            /*val stdOutHash = HashService.computeHash(stdOutFile)
            if (stdOutHash != message.hash)
              logger.log(WARNING, "The standard output has been corrupted durring the transfert.")
            */
           
            System.out.synchronized {
              System.out.println("-----------------" + description + " on remote host-----------------")
              val fis = new FileInputStream(stdOutFile)
              try fis.copy(System.out) finally fis.close
              System.out.println("-------------------------------------------------------")
            }
          } finally stdOutFile.delete
        
        } catch {
          case(e: IOException) => 
            GetResultActor.logger.log(WARNING, description + " transfer has failed.")
            GetResultActor.logger.log(FINE, "Stack of the error durring tranfert" , e)
        }
      case None => 
    }
  }

  private def getContextResults(resultPath: FileMessage, communicationStorage: Storage, token: AccessToken): ContextResults = {
    import communicationStorage.path
    if (resultPath == null) throw new InternalProcessingError("Context results path is null")
    val contextResutsFileCache = signalDownload(path.cacheUnziped(resultPath.path, token), path.toURI(resultPath.path), communicationStorage)
    if(HashService.computeHash(contextResutsFileCache) != resultPath.hash) throw new InternalProcessingError("Results have been corrupted durring the transfer.")
    
    try SerializerService.deserializeAndExtractFiles(contextResutsFileCache)
    finally contextResutsFileCache.delete
  }
  
}
