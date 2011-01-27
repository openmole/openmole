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

import java.io.File
import java.util.TreeSet
import java.util.UUID
import java.util.concurrent.Callable
import java.util.logging.Logger
import org.openmole.core.batch.file.URIFile
import org.openmole.core.batch.internal.Activator._
import org.openmole.core.batch.replication.ReplicaCatalog
import org.openmole.core.implementation.execution.JobRegistry
import org.openmole.core.batch.message.ExecutionMessage
import org.openmole.core.batch.message.FileMessage
import org.openmole.core.batch.message.JobForRuntime
import org.openmole.core.batch.message.ReplicatedFile
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.control.BatchStorageControl
import org.openmole.core.batch.file.GZURIFile
import org.openmole.core.batch.file.IURIFile
import org.openmole.core.model.job.IJob
import org.openmole.core.model.job.IMoleJob


import scala.io.Source._
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

class CopyToEnvironment(environment: BatchEnvironment, job: IJob) extends Callable[CopyToEnvironmentResult] {

  private def initCommunication(): CopyToEnvironmentResult = {
    val jobFile = workspace.newFile("job", ".xml")
    
    try {
      val serializationResult = serializer.serializeGetPluginClassAndFiles(createJobForRuntime, jobFile)
      val storage = environment.selectAStorage(serializationResult._1)

      val communicationStorage = storage._1
      val token = storage._2

      try {
        val communicationDir = communicationStorage.tmpSpace(token).mkdir(UUID.randomUUID.toString + '/', token)
            
        val inputFile = new GZURIFile(communicationDir.newFileInDir("job", ".in"))
        val outputFile = new GZURIFile(communicationDir.newFileInDir("job", ".out"))

        val runtime = replicateTheRuntime(token, communicationStorage, communicationDir)

        val executionMessage = createExecutionMessage(jobFile,serializationResult, token, communicationStorage, communicationDir)

        /* ---- upload the execution message ----*/

        val executionMessageFile = workspace.newFile("job", ".xml")
        serializer.serialize(executionMessage, executionMessageFile)

        val executionMessageURIFile = new URIFile(executionMessageFile)
        URIFile.copy(executionMessageURIFile, inputFile, token)

        executionMessageFile.delete
            
        return new CopyToEnvironmentResult(communicationStorage, communicationDir, inputFile, outputFile, runtime)
      } finally {
        BatchStorageControl.usageControl(communicationStorage.description).releaseToken(token)
      }
    } finally {
      jobFile.delete
    }
  }

  override def call: CopyToEnvironmentResult = {
    initCommunication
  }

  def toReplicatedFile(file: File, storage: BatchStorage, token: AccessToken): ReplicatedFile = {
    val isDir = file.isDirectory
    var toReplicate = file
    val toReplicatePath = file.getAbsoluteFile
    val moleExecution = JobRegistry(job)

    //Hold cache to avoid gc and file deletion
    val cache = if (isDir) {
      //Logger.getLogger(classOf[CopyToEnvironment].getName).info("Archive for dir " + file)
      val cache = fileService.archiveForDir(file, moleExecution)
      toReplicate = cache.file(false)
      cache
    } else null

    val hash = fileService.hash(toReplicate, moleExecution)
    val replica = ReplicaCatalog.uploadAndGet(toReplicate, toReplicatePath, hash, storage, token)
    new ReplicatedFile(file, isDir, hash, replica.destination)
  }


  def replicateTheRuntime(token: AccessToken, communicationStorage: BatchStorage, communicationDir: IURIFile): Runtime = {
    val environmentPluginReplica = new ListBuffer[IURIFile]

    val environmentPlugins = pluginManager.getPluginAndDependanciesForClass(environment.getClass)
    val runtimeFile = environment.runtime

    for (environmentPlugin <- environmentPlugins) {     
      val replicatedFile = toReplicatedFile(environmentPlugin, communicationStorage, token)
      val pluginURIFile = replicatedFile.replica
             
      environmentPluginReplica += pluginURIFile
    }

    val runtimeReplica = toReplicatedFile(runtimeFile, communicationStorage, token).replica
        
    val authenticationFile = workspace.newFile("envrionmentAuthentication", ".xml")
     
    serializer.serialize(communicationStorage.authentication.asInstanceOf[AnyRef], authenticationFile)
    
    //println("Authentication " + authenticationFile.getAbsolutePath)
  
    val authenticationURIFile = new GZURIFile(communicationDir.newFileInDir("authentication", ".xml"))
    URIFile.copy(authenticationFile, authenticationURIFile, token)
    authenticationFile.delete
        
    return new Runtime(runtimeReplica, environmentPluginReplica.toList, authenticationURIFile)
  }

  def serialize(jobForRuntime: JobForRuntime, jobFile: File) = {
    serializer.serializeGetPluginClassAndFiles(jobForRuntime, jobFile)
  }
  
  def createExecutionMessage(jobFile: File, serializationResult: (Iterable[File], Iterable[Class[_]]), token: AccessToken, communicationStorage: BatchStorage, communicationDir: IURIFile): ExecutionMessage = {

    val jobURIFile = new URIFile(jobFile)
    val jobForRuntimeFile = new GZURIFile(communicationDir.newFileInDir("job", ".xml"))

    URIFile.copy(jobURIFile, jobForRuntimeFile, token)
    val jobHash = hashService.computeHash(jobFile)

    val plugins = new TreeSet[File]
    val pluginReplicas = new ListBuffer[ReplicatedFile]

    for (c <- serializationResult._2) {
      for (f <- pluginManager.getPluginAndDependanciesForClass(c)) {
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

    new ExecutionMessage(pluginReplicas, files, new FileMessage(jobForRuntimeFile, jobHash), communicationDir)
  
  }

  def createJobForRuntime: JobForRuntime = {
       
    val jobs = new ListBuffer[IMoleJob]

    for (moleJob <- job.moleJobs) {
      moleJob.synchronized {
        if (!moleJob.isFinished) {
          jobs += moleJob
        }
      }
    }

    new JobForRuntime(jobs.toList)
  }
}
