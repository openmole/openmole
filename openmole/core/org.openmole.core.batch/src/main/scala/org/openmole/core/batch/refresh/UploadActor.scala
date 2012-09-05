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
import com.db4o.ObjectContainer
import com.ice.tar.TarOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.Callable
import org.openmole.core.batch.file.URIFile
import org.openmole.core.batch.message.RunnableTask
import org.openmole.core.batch.replication.ReplicaCatalog
import org.openmole.core.batch.message.ExecutionMessage
import org.openmole.core.batch.message.FileMessage
import org.openmole.core.batch.message.ReplicatedFile
import java.util.logging.Level
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.control.UsageControl
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.core.batch.environment.BatchEnvironment.{ signalDownload, signalUpload }
import org.openmole.core.batch.environment.SerializedJob
import org.openmole.core.batch.environment.{ Storage, Runtime }
import org.openmole.core.batch.file.GZURIFile
import org.openmole.core.batch.file.IURIFile
import org.openmole.core.model.execution.IEnvironment
import org.openmole.core.model.job.IJob
import org.openmole.misc.tools.io.FileUtil
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.tools.io.TarArchiver._

import scala.io.Source._
import org.openmole.core.serializer.SerializerService
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.fileservice.FileService
import org.openmole.misc.hashservice.HashService
import org.openmole.misc.pluginmanager.PluginManager
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.workspace.Workspace
import scala.collection.JavaConversions._
import scala.collection.immutable.TreeSet
import scala.collection.mutable.HashSet
import org.openmole.misc.hashservice.HashService._
import org.openmole.core.model.execution.ExecutionState._

//import actors.futures._

object UploadActor extends Logger

import UploadActor._

class UploadActor(jobManager: ActorRef) extends Actor {
  def receive = {
    case Upload(job) ⇒
      if (!job.state.isFinal) {
        try {
          val sj = initCommunication(job.environment, job.job)
          jobManager ! Uploaded(job, sj)
        } catch {
          case e ⇒
            logger.log(FINE, "Exception raised durring job upload.", e)
            jobManager ! Error(job, e)
            jobManager ! Kill(job)
        }
      }
      System.runFinalization
  }

  private def initCommunication(environment: BatchEnvironment, job: IJob): SerializedJob = {
    val jobFile = Workspace.newFile("job", ".tar")

    try {
      val (serializationFile, serializatonPlugins) = serializeJob(jobFile, job)

      val serialisationPluginFiles = new TreeSet[File] ++ serializatonPlugins.flatMap { PluginManager.pluginsForClass }

      val storage = environment.selectAStorage(
        serializationFile +
          environment.runtime +
          environment.jvmLinuxI386 +
          environment.jvmLinuxX64 ++
          environment.plugins ++
          serialisationPluginFiles)

      val (communicationStorage, token) = storage

      try ReplicaCatalog.withClient { implicit client ⇒
        val communicationDir = communicationStorage.tmpSpace(token).mkdir(UUID.randomUUID.toString + '/', token)

        val inputFile = new GZURIFile(communicationDir.newFileInDir("job", ".in"))
        val runtime = replicateTheRuntime(job, environment, token, communicationStorage, communicationDir)

        val executionMessage = createExecutionMessage(
          job,
          jobFile,
          serializationFile,
          serialisationPluginFiles,
          token,
          communicationStorage,
          communicationDir)

        /* ---- upload the execution message ----*/
        val executionMessageFile = Workspace.newFile("job", ".xml")
        try {
          SerializerService.serialize(executionMessage, executionMessageFile)
          signalUpload(URIFile.copy(executionMessageFile, inputFile, token), executionMessageFile, communicationStorage)
        } finally executionMessageFile.delete

        new SerializedJob(communicationStorage, communicationDir.path, inputFile.path, runtime)
      } finally UsageControl.get(communicationStorage.description).releaseToken(token)
    } finally jobFile.delete
  }

  def serializeJob(file: File, job: IJob) = {
    val files = new HashSet[File]
    val classes = new HashSet[Class[_]]

    val tos = new TarOutputStream(new FileOutputStream(file))
    try {
      for (moleJob ← job.moleJobs) moleJob.synchronized {
        if (!moleJob.finished) {
          val moleJobFile = Workspace.newFile("job", ".tar")
          try {
            val serializationResult =
              SerializerService.serializeGetPluginClassAndFiles(
                RunnableTask(moleJob),
                moleJobFile)

            files ++= serializationResult.files
            classes ++= serializationResult.classes

            tos.addFile(moleJobFile, UUID.randomUUID.toString)
          } finally moleJobFile.delete
        }
      }
    } finally tos.close
    (files, classes)
  }

  def toReplicatedFile(job: IJob, file: File, storage: Storage, token: AccessToken)(implicit objectContainer: ObjectContainer): ReplicatedFile = {
    val isDir = file.isDirectory
    var toReplicate = file
    val toReplicatePath = file.getAbsoluteFile

    //Hold cache to avoid gc and file deletion
    val cache = if (isDir) {
      val cache = FileService.archiveForDir(job.moleExecution, file)
      toReplicate = cache.file(false)
      cache
    } else null

    val hash = FileService.hash(job.moleExecution, toReplicate).toString
    val replica = ReplicaCatalog.uploadAndGet(toReplicate, toReplicatePath, hash, storage, token)
    new ReplicatedFile(file, isDir, hash, replica.destinationURIFile.path, file.mode)
  }

  def replicateTheRuntime(
    job: IJob,
    environment: BatchEnvironment,
    token: AccessToken,
    communicationStorage: Storage,
    communicationDir: IURIFile)(implicit objectContainer: ObjectContainer) = {

    val environmentPluginPath = environment.plugins.view.map { p ⇒ toReplicatedFile(job, p, communicationStorage, token) }.map { f ⇒ new FileMessage(f) }
    val runtimeFileMessage = new FileMessage(toReplicatedFile(job, environment.runtime, communicationStorage, token))
    val jvmLinuxI386FileMessage = new FileMessage(toReplicatedFile(job, environment.jvmLinuxI386, communicationStorage, token))
    val jvmLinuxX64FileMessage = new FileMessage(toReplicatedFile(job, environment.jvmLinuxX64, communicationStorage, token))

    val authReplication = new FileMessage(toReplicatedFile(job, environment.serializedAuthentication, communicationStorage, token))

    new Runtime(
      runtimeFileMessage,
      environmentPluginPath.force,
      authReplication,
      jvmLinuxI386FileMessage,
      jvmLinuxX64FileMessage)
  }

  def createExecutionMessage(
    job: IJob,
    jobFile: File,
    serializationFile: Iterable[File],
    serializationPlugin: Iterable[File],
    token: AccessToken,
    communicationStorage: Storage,
    communicationDir: IURIFile)(implicit objectContainer: ObjectContainer): ExecutionMessage = {
    val jobForRuntimeFile = new GZURIFile(communicationDir.newFileInDir("job", ".tar"))

    signalUpload(URIFile.copy(jobFile, jobForRuntimeFile, token), jobFile, communicationStorage)
    val jobHash = HashService.computeHash(jobFile).toString

    val pluginReplicas = serializationPlugin.view.map { p ⇒ { toReplicatedFile(job, p, communicationStorage, token) } }
    val files = serializationFile.view.map { f ⇒ { toReplicatedFile(job, f, communicationStorage, token) } }

    new ExecutionMessage(
      pluginReplicas.force,
      files.force,
      new FileMessage(jobForRuntimeFile.URI.getPath, jobHash),
      communicationDir.URI.getPath)
  }

}
