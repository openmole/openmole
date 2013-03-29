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
import org.openmole.core.batch.message._
import org.openmole.core.batch.replication._
import org.openmole.core.batch.storage._
import org.openmole.core.batch.control._
import org.openmole.core.batch.environment._
import org.openmole.core.batch.environment.BatchEnvironment.{ signalDownload, signalUpload }
import org.openmole.core.model.execution._
import org.openmole.core.model.job._
import org.openmole.misc.tools.io.FileUtil
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.tools.io.TarArchiver._

import scala.io.Source._
import org.openmole.core.serializer._
import org.openmole.misc.eventdispatcher._
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
import org.openmole.misc.exception.UserBadDataError

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
          case e: Throwable ⇒ signalError(job, e)
        }
      }
      System.runFinalization
  }

  private def signalError(job: BatchExecutionJob, e: Throwable) = {
    jobManager ! Error(job, e)
    jobManager ! Upload(job)
  }

  private def initCommunication(environment: BatchEnvironment, job: IJob): SerializedJob = {
    val jobFile = Workspace.newFile("job", ".tar")

    try {
      val (serializationFile, serialisationPluginFiles) = serializeJob(jobFile, job)

      val (storage, token) = environment.selectAStorage(
        (serializationFile +
          environment.runtime +
          environment.jvmLinuxI386 +
          environment.jvmLinuxX64 ++
          environment.plugins ++
          serialisationPluginFiles).map(f ⇒ f -> FileService.hash(job.moleExecution, f)))

      implicit val t = token
      try ReplicaCatalog.withClient { implicit client ⇒
        val communicationPath = storage.child(storage.tmpDir, UUID.randomUUID.toString)
        storage.makeDir(communicationPath)

        val inputPath = storage.child(communicationPath, Storage.uniqName("job", ".in"))

        val runtime = replicateTheRuntime(job, environment, storage)

        val executionMessage = createExecutionMessage(
          job,
          jobFile,
          serializationFile,
          serialisationPluginFiles,
          storage,
          communicationPath)

        /* ---- upload the execution message ----*/
        val executionMessageFile = Workspace.newFile("job", ".xml")
        try {
          SerializerService.serialize(executionMessage, executionMessageFile)
          signalUpload(
            storage.uploadGZ(executionMessageFile, inputPath), inputPath, storage)
        } finally executionMessageFile.delete

        new SerializedJob(storage, communicationPath, inputPath, runtime)
      } finally storage.releaseToken(token)
    } finally jobFile.delete
  }

  def serializeJob(file: File, job: IJob) = {
    var files = new TreeSet[File]
    var plugins = new TreeSet[File]

    val tos = new TarOutputStream(new FileOutputStream(file))
    try {
      for (moleJob ← job.moleJobs) moleJob.synchronized {
        if (!moleJob.finished) {
          val moleJobFile = Workspace.newFile("job", ".tar")
          try {
            val serializationResult =
              SerializerService.serializeGetPluginsAndFiles(
                RunnableTask(moleJob),
                moleJobFile)

            files ++= serializationResult.files
            plugins ++= serializationResult.plugins

            tos.addFile(moleJobFile, UUID.randomUUID.toString)
          } finally moleJobFile.delete
        }
      }
    } finally tos.close
    (files, plugins)
  }

  def toReplicatedFile(job: IJob, file: File, storage: StorageService)(implicit token: AccessToken, objectContainer: ObjectContainer): ReplicatedFile = {
    if(!file.exists) throw new UserBadDataError(s"File/dir $file is requiered but doesn't exist.")

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
    val replica = ReplicaCatalog.uploadAndGet(toReplicate, toReplicatePath, hash, storage)
    new ReplicatedFile(file, isDir, hash, replica.path, file.mode)
  }

  def replicateTheRuntime(
    job: IJob,
    environment: BatchEnvironment,
    storage: StorageService)(implicit token: AccessToken, objectContainer: ObjectContainer) = {

    val environmentPluginPath = environment.plugins.map { p ⇒ toReplicatedFile(job, p, storage) }.map { f ⇒ new FileMessage(f) }
    val runtimeFileMessage = new FileMessage(toReplicatedFile(job, environment.runtime, storage))
    val jvmLinuxI386FileMessage = new FileMessage(toReplicatedFile(job, environment.jvmLinuxI386, storage))
    val jvmLinuxX64FileMessage = new FileMessage(toReplicatedFile(job, environment.jvmLinuxX64, storage))

    val storageReplication = new FileMessage(toReplicatedFile(job, storage.serializedRemoteStorage, storage))

    new Runtime(
      storageReplication,
      runtimeFileMessage,
      environmentPluginPath,
      jvmLinuxI386FileMessage,
      jvmLinuxX64FileMessage)
  }

  def createExecutionMessage(
    job: IJob,
    jobFile: File,
    serializationFile: Iterable[File],
    serializationPlugin: Iterable[File],
    storage: StorageService,
    path: String)(implicit token: AccessToken, objectContainer: ObjectContainer): ExecutionMessage = {
    val jobForRuntimePath = storage.child(path, Storage.uniqName("job", ".tgz"))

    signalUpload(
      storage.uploadGZ(jobFile, jobForRuntimePath), jobForRuntimePath, storage)
    val jobHash = HashService.computeHash(jobFile).toString

    val pluginReplicas = serializationPlugin.map { toReplicatedFile(job, _, storage) }
    val files = serializationFile.map { toReplicatedFile(job, _, storage) }

    new ExecutionMessage(
      pluginReplicas,
      files,
      new FileMessage(jobForRuntimePath, jobHash),
      path)
  }

}
