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

import com.ice.tar.TarOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.TreeSet
import java.util.UUID
import java.util.concurrent.Callable
import org.openmole.core.batch.file.URIFile
import org.openmole.core.batch.replication.ReplicaCatalog
import org.openmole.core.implementation.execution.JobRegistry
import org.openmole.core.batch.message.ExecutionMessage
import org.openmole.core.batch.message.FileMessage
import org.openmole.core.batch.message.ReplicatedFile
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.control.BatchStorageControl
import org.openmole.core.batch.file.GZURIFile
import org.openmole.core.batch.file.IURIFile
import org.openmole.core.model.job.IJob
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.tools.io.TarArchiver._

import scala.io.Source._
import org.openmole.core.serializer.SerializerService
import org.openmole.misc.fileservice.internal.FileService
import org.openmole.misc.hashservice.HashService
import org.openmole.misc.pluginmanager.PluginManager
import org.openmole.misc.workspace.Workspace
import scala.collection.JavaConversions._
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer

class CopyToEnvironment(environment: BatchEnvironment, job: IJob) extends Callable[CopyToEnvironmentResult] {

  private def initCommunication: CopyToEnvironmentResult = {
    val jobFile = Workspace.newFile("job", ".tar")
    
    try {
      val serializationResult = serializeJob(jobFile)
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

        val executionMessageFile = Workspace.newFile("job", ".xml")
        try {
          SerializerService.serialize(executionMessage, executionMessageFile)
          URIFile.copy(new URIFile(executionMessageFile), inputFile, token)
        } finally executionMessageFile.delete
            
        new CopyToEnvironmentResult(communicationStorage, communicationDir, inputFile, outputFile, runtime)
      } finally BatchStorageControl.usageControl(communicationStorage.description).releaseToken(token)
    } finally jobFile.delete
  }

  def serializeJob(file: File) = {
    val files = new HashSet[File]
    val classes = new HashSet[Class[_]]
    
    val tos = new TarOutputStream(new FileOutputStream(file))   
    try {
      for(moleJob <- job.moleJobs) moleJob.synchronized {    
        if(!moleJob.isFinished) {
          val moleJobFile = Workspace.newFile("job", ".tar")
          try {
            val serializationResult = SerializerService.serializeGetPluginClassAndFiles(moleJob, moleJobFile)
            
            files ++= serializationResult.files
            classes ++= serializationResult.classes
          
            tos.addFile(moleJobFile, UUID.randomUUID.toString)
          } finally moleJobFile.delete
        }
      }
    } finally tos.close
    (files, classes)
  }
  
  
  override def call: CopyToEnvironmentResult = initCommunication

  def toReplicatedFile(file: File, storage: BatchStorage, token: AccessToken): ReplicatedFile = {
    val isDir = file.isDirectory
    var toReplicate = file
    val toReplicatePath = file.getAbsoluteFile
    val moleExecution = JobRegistry(job)

    //Hold cache to avoid gc and file deletion
    val cache = if (isDir) {
      val cache = FileService.archiveForDir(file, moleExecution)
      toReplicate = cache.file(false)
      cache
    } else null

    val hash = FileService.hash(toReplicate, moleExecution)
    val replica = ReplicaCatalog.uploadAndGet(toReplicate, toReplicatePath, hash, storage, token)
    new ReplicatedFile(file, isDir, hash, replica.destinationURIFile)
  }


  def replicateTheRuntime(token: AccessToken, communicationStorage: BatchStorage, communicationDir: IURIFile): Runtime = {
    val environmentPluginReplica = new ListBuffer[IURIFile]

    val environmentPlugins = PluginManager.pluginsForClass(environment.getClass)
    val runtimeFile = environment.runtime

    for (environmentPlugin <- environmentPlugins) environmentPluginReplica += toReplicatedFile(environmentPlugin, communicationStorage, token).replica
            
    val runtimeReplica = toReplicatedFile(runtimeFile, communicationStorage, token).replica
    
    val authenticationURIFile = new GZURIFile(communicationDir.newFileInDir("authentication", ".xml"))
    val authenticationFile = Workspace.newFile("environmentAuthentication", ".xml")
    try {
      SerializerService.serialize(communicationStorage.environment.authentication, authenticationFile)
      URIFile.copy(authenticationFile, authenticationURIFile, token)
    } finally authenticationFile.delete
        
    new Runtime(runtimeReplica, environmentPluginReplica.toList, authenticationURIFile)
  }
  
  def createExecutionMessage(jobFile: File, serializationResult: (Iterable[File], Iterable[Class[_]]), token: AccessToken, communicationStorage: BatchStorage, communicationDir: IURIFile): ExecutionMessage = {
    val jobURIFile = new URIFile(jobFile)
    val jobForRuntimeFile = new GZURIFile(communicationDir.newFileInDir("job", ".tar"))

    URIFile.copy(jobURIFile, jobForRuntimeFile, token)
    val jobHash = HashService.computeHash(jobFile)

    val plugins = new TreeSet[File]
    val pluginReplicas = new ListBuffer[ReplicatedFile]

    for (c <- serializationResult._2) plugins ++= PluginManager.pluginsForClass(c)
    for (f <- plugins) pluginReplicas += toReplicatedFile(f, communicationStorage, token)

    val files = new ListBuffer[ReplicatedFile]
    for(file <- serializationResult._1) files += toReplicatedFile(file, communicationStorage, token)

    new ExecutionMessage(pluginReplicas, files, new FileMessage(jobForRuntimeFile, jobHash), communicationDir)
  }

}
