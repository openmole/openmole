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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.batch.environment

import com.ice.tar.TarOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.util.TreeSet
import java.util.UUID
import java.util.concurrent.Callable
import org.openmole.core.batch.file.URIFile
import org.openmole.core.batch.replication.ReplicaCatalog
import org.openmole.core.batch.message.ExecutionMessage
import org.openmole.core.batch.message.FileMessage
import org.openmole.core.batch.message.ReplicatedFile
import java.util.concurrent.atomic.AtomicLong
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.control.StorageControl
import org.openmole.core.batch.file.GZURIFile
import org.openmole.core.batch.file.IURIFile
import org.openmole.core.model.job.IJob
import org.openmole.misc.tools.io.FileUtil
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
import org.openmole.misc.hashservice.HashService._
import BatchEnvironment._

class CopyToEnvironment(environment: BatchEnvironment, job: IJob) extends Callable[SerializedJob] {
  
  private def initCommunication: SerializedJob = {
    val jobFile = Workspace.newFile("job", ".tar")
    
    try {
      val (serializationFile, serializatonPlugins) = serializeJob(jobFile)
      val serialisationPluginFiles = new TreeSet[File] ++ serializatonPlugins.flatMap{PluginManager.pluginsForClass}
      
      val storage = environment.selectAStorage(serializationFile + 
                                               environment.runtime +
                                               environment.jvmLinuxI386 +
                                               environment.jvmLinuxX64 ++
                                               environment.plugins ++
                                               serialisationPluginFiles)

      val communicationStorage = storage._1
      val token = storage._2

      try {
        val communicationDir = communicationStorage.tmpSpace(token).mkdir(UUID.randomUUID.toString + '/', token)
            
        val inputFile = new GZURIFile(communicationDir.newFileInDir("job", ".in"))
        val runtime = replicateTheRuntime(token, communicationStorage, communicationDir)

        val executionMessage = createExecutionMessage(jobFile, 
                                                      serializationFile, 
                                                      serialisationPluginFiles, 
                                                      token, 
                                                      communicationStorage, 
                                                      communicationDir)

        /* ---- upload the execution message ----*/
        val executionMessageFile = Workspace.newFile("job", ".xml")
        try {
          SerializerService.serialize(executionMessage, executionMessageFile)
          URIFile.copy(executionMessageFile, inputFile, token)
        } finally executionMessageFile.delete
            
        new SerializedJob(communicationStorage, communicationDir.path, inputFile.path, runtime)
      } finally StorageControl.usageControl(communicationStorage.description).releaseToken(token)
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
  
  
  override def call: SerializedJob = initCommunication

  def toReplicatedFile(file: File, storage: Storage, token: AccessToken): ReplicatedFile = {
    val isDir = file.isDirectory
    var toReplicate = file
    val toReplicatePath = file.getAbsoluteFile

    //Hold cache to avoid gc and file deletion
    val cache = if (isDir) {
      val cache = FileService.archiveForDir(file, job.executionId)
      toReplicate = cache.file(false)
      cache
    } else null

    val hash = FileService.hash(toReplicate, job.executionId).toString
    val replica = ReplicaCatalog.uploadAndGet(toReplicate, toReplicatePath, hash, storage, token)
    new ReplicatedFile(file, isDir, hash, replica.destinationURIFile.path, file.mode)
  }

  def replicateTheRuntime(token: AccessToken, 
                          communicationStorage: Storage, 
                          communicationDir: IURIFile): Runtime = {
   

    val environmentPluginPath = environment.plugins.map{toReplicatedFile(_, communicationStorage, token)}.map{new FileMessage(_)}    
    val runtimeFileMessage = new FileMessage(toReplicatedFile(environment.runtime, communicationStorage, token))
    val jvmLinuxI386FileMessage = new FileMessage(toReplicatedFile(environment.jvmLinuxI386, communicationStorage, token))
    val jvmLinuxX64FileMessage = new FileMessage(toReplicatedFile(environment.jvmLinuxX64, communicationStorage, token))

    val authenticationURIFile = new GZURIFile(communicationDir.newFileInDir("authentication", ".xml"))
    val authenticationFile = Workspace.newFile("environmentAuthentication", ".xml")
    
    val authReplication = try {
      SerializerService.serialize(communicationStorage.environment.authentication, authenticationFile)
      URIFile.copy(authenticationFile, authenticationURIFile, token)
      new FileMessage(authenticationURIFile.URI.getPath, authenticationFile.hash.toString)
    } finally authenticationFile.delete
        
    
    new Runtime(runtimeFileMessage, environmentPluginPath, authReplication, jvmLinuxI386FileMessage, jvmLinuxX64FileMessage)
  }
  
  def createExecutionMessage(jobFile: File, serializationFile: Iterable[File], serializationPlugin: Iterable[File], token: AccessToken, communicationStorage: Storage, communicationDir: IURIFile): ExecutionMessage = {
    val jobForRuntimeFile = new GZURIFile(communicationDir.newFileInDir("job", ".tar"))

    URIFile.copy(jobFile, jobForRuntimeFile, token)
    val jobHash = HashService.computeHash(jobFile).toString

    val pluginReplicas = serializationPlugin.map{toReplicatedFile(_, communicationStorage, token)}.toList
    val files = serializationFile.map{toReplicatedFile(_, communicationStorage, token)}.toList
  
    new ExecutionMessage(pluginReplicas, files, new FileMessage(jobForRuntimeFile.URI.getPath, jobHash), communicationDir.URI.getPath)
  }

}
