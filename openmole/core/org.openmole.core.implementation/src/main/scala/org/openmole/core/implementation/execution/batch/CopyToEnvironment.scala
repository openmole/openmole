/*
 * Copyright (C) 2010 reuillon
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

package org.openmole.core.implementation.execution.batch

import java.io.File
import java.util.TreeSet
import java.util.UUID
import java.util.concurrent.Callable
import java.util.logging.Logger
import org.openmole.core.file.GZURIFile
import org.openmole.core.file.URIFile
import org.openmole.core.implementation.execution.JobRegistry
import org.openmole.core.implementation.internal.Activator
import org.openmole.core.implementation.message.ExecutionMessage
import org.openmole.core.implementation.message.FileMessage
import org.openmole.core.implementation.message.JobForRuntime
import org.openmole.core.implementation.message.ReplicatedFile
import org.openmole.core.model.execution.batch.IAccessToken
import org.openmole.core.model.execution.batch.IBatchStorage
import org.openmole.core.model.execution.batch.IRuntime
import org.openmole.core.model.file.IURIFile
import org.openmole.core.model.job.IJob
import org.openmole.core.model.job.IMoleJob

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

class CopyToEnvironment(environment: BatchEnvironment[_], job: IJob) extends Callable[CopyToEnvironmentResult] {

  private def initCommunication(): CopyToEnvironmentResult = {
        
    val storage = environment.getAStorage

    val communicationStorage = storage._1
    val token = storage._2

    try {
      val communicationDir = communicationStorage.tmpSpace(token).mkdir(UUID.randomUUID.toString + '/', token)
            
      val inputFile = new GZURIFile(communicationDir.newFileInDir("job", ".in"))
      val outputFile = new GZURIFile(communicationDir.newFileInDir("job", ".out"))

      val runtime = replicateTheRuntime(token, communicationStorage, communicationDir)

      val jobForRuntime = createJobForRuntime(token, communicationStorage, communicationDir)
      val executionMessage = createExecutionMessage(jobForRuntime, token, communicationStorage, communicationDir)

      /* ---- upload the execution message ----*/

      val executionMessageFile = Activator.getWorkspace.newFile("job", ".xml")
      Activator.getSerializer.serialize(executionMessage, executionMessageFile)

      val executionMessageURIFile = new URIFile(executionMessageFile)
      URIFile.copy(executionMessageURIFile, inputFile, token)

      executionMessageFile.delete
            
      return new CopyToEnvironmentResult(communicationStorage, communicationDir, inputFile, outputFile, runtime)
    } finally {
      Activator.getBatchRessourceControl.getController(communicationStorage.description).getUsageControl.releaseToken(token)
    }
  }

  override def call: CopyToEnvironmentResult = {
    initCommunication
  }

  def toReplicatedFile(file: File, storage: IBatchStorage[_,_], token: IAccessToken): ReplicatedFile = {
    val isDir = file.isDirectory
    var toReplicate = file
    val toReplicatePath = file.getAbsoluteFile
    val moleExecution = JobRegistry.getInstance.getMoleExecutionForJob(job)

    //Hold cache to avoid gc and file deletion
    val cache = if (isDir) {
      val cache = Activator.getFileService.getArchiveForDir(file, moleExecution)
      toReplicate = cache.getFile(false)
      cache
    } else null

    val hash = Activator.getFileService.getHashForFile(toReplicate, moleExecution)
    val replica = Activator.getReplicaCatalog.uploadAndGet(toReplicate, toReplicatePath, hash, storage, token)
    new ReplicatedFile(file, isDir, hash, replica.getDestination)
  }


  def replicateTheRuntime(token: IAccessToken, communicationStorage: IBatchStorage[_,_], communicationDir: IURIFile): IRuntime = {
    val environmentPluginReplica = new ListBuffer[IURIFile]

    val environmentPlugins = Activator.getPluginManager().getPluginAndDependanciesForClass(environment.getClass)
    val runtimeFile = environment.runtime

    for (environmentPlugin <- environmentPlugins) {     
      val replicatedFile = toReplicatedFile(environmentPlugin, communicationStorage, token)
      val pluginURIFile = replicatedFile.replica
             
      environmentPluginReplica += pluginURIFile
    }

    val runtimeReplica = toReplicatedFile(runtimeFile, communicationStorage, token).replica
        
    val authenticationFile = Activator.getWorkspace.newFile("envrionmentAuthentication", ".xml")
    Activator.getSerializer.serialize(communicationStorage.authentication.asInstanceOf[AnyRef], authenticationFile)
    val authenticationURIFile = new GZURIFile(communicationDir.newFileInDir("authentication", ".xml"))
    URIFile.copy(authenticationFile, authenticationURIFile, token)
    authenticationFile.delete
        
    return new Runtime(runtimeReplica, environmentPluginReplica.toList, authenticationURIFile)
  }

  def createExecutionMessage(jobForRuntime: JobForRuntime, token: IAccessToken, communicationStorage: IBatchStorage[_,_], communicationDir: IURIFile): ExecutionMessage = {

    val jobFile = Activator.getWorkspace.newFile("job", ".xml")
    val serializationResult = Activator.getSerializer.serializeGetPluginClassAndFiles(jobForRuntime, jobFile)
        
    val jobURIFile = new URIFile(jobFile)
    val jobForRuntimeFile = new GZURIFile(communicationDir.newFileInDir("job", ".xml"))

    URIFile.copy(jobURIFile, jobForRuntimeFile, token)
    val jobHash = Activator.getHashService.computeHash(jobFile)

    jobURIFile.remove(false)

    val plugins = new TreeSet[File]
    val pluginReplicas = new ListBuffer[ReplicatedFile]

    for (c <- serializationResult._2) {
      for (f <- Activator.getPluginManager.getPluginAndDependanciesForClass(c)) {
        plugins += f
      }
    }

    for (f <- plugins) {
      val replicatedPlugin = toReplicatedFile(f, communicationStorage, token)
      pluginReplicas += replicatedPlugin
    }
        
    val files = new ListBuffer[ReplicatedFile]
        
    for(file <- serializationResult._1) {
      files += toReplicatedFile(file, communicationStorage, token)
    }

    new ExecutionMessage(pluginReplicas, files, new FileMessage(jobForRuntimeFile, jobHash), communicationDir);
  }

  def createJobForRuntime(token: IAccessToken, communicationStorage: IBatchStorage[_,_], communicationDir: IURIFile): JobForRuntime = {
       
    val jobs = new ListBuffer[IMoleJob]

    for (moleJob <- job.getMoleJobs) {
      moleJob.synchronized {
        if (!moleJob.isFinished) {
          jobs += moleJob
        }
      }
    }

    new JobForRuntime(jobs.toList)
  }
}
