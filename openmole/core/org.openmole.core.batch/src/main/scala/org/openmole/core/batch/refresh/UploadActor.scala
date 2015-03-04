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
import com.ice.tar.TarOutputStream
import java.io.File
import java.util.UUID
import org.openmole.core.batch.message._
import org.openmole.core.batch.replication._
import org.openmole.core.batch.storage._
import org.openmole.core.batch.control._
import org.openmole.core.batch.environment._
import org.openmole.core.batch.environment.BatchEnvironment.{ signalUpload }
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.fileservice.FileService
import org.openmole.core.tools.io.{ HashService, FileUtil, TarArchiver }
import org.openmole.core.workflow.job._
import FileUtil._
import TarArchiver._

import org.openmole.core.serializer._
import org.openmole.core.workspace.Workspace
import scala.collection.immutable.TreeSet
import scala.slick.driver.H2Driver.simple._

class UploadActor(jobManager: ActorRef) extends Actor {

  def receive = withRunFinalization {
    case msg @ Upload(job) ⇒
      if (!job.state.isFinal) {
        try {
          val sj = initCommunication(job.environment, job.job)
          jobManager ! Uploaded(job, sj)
        }
        catch {
          case e: Throwable ⇒
            jobManager ! Error(job, e)
            jobManager ! msg
        }
      }
  }

  private def initCommunication(environment: BatchEnvironment, job: Job): SerializedJob = Workspace.withTmpFile("job", ".tar") { jobFile ⇒

    val (serializationFiles, serialisationPluginFiles) = serializeJob(jobFile, job)

    val (storage, token) = environment.selectAStorage(
      (serializationFiles +
        environment.runtime +
        environment.jvmLinuxI386 +
        environment.jvmLinuxX64 ++
        environment.plugins ++
        serialisationPluginFiles).map(f ⇒ f -> FileService.hash(job.moleExecution, f)))

    implicit val t = token
    try ReplicaCatalog.withSession { implicit session ⇒
      val communicationPath = storage.child(storage.tmpDir, UUID.randomUUID.toString)
      storage.makeDir(communicationPath)

      val inputPath = storage.child(communicationPath, Storage.uniqName("job", ".in"))

      val runtime = replicateTheRuntime(job, environment, storage)

      val executionMessage = createExecutionMessage(
        job,
        jobFile,
        serializationFiles,
        serialisationPluginFiles,
        storage,
        communicationPath)

      /* ---- upload the execution message ----*/
      Workspace.withTmpFile("job", ".xml") { executionMessageFile ⇒
        SerialiserService.serialise(executionMessage, executionMessageFile)
        signalUpload(storage.uploadGZ(executionMessageFile, inputPath), inputPath, storage)
      }

      SerializedJob(storage, communicationPath, inputPath, runtime)
    } finally storage.releaseToken(token)
  }

  def serializeJob(file: File, job: Job) = {
    var files = new TreeSet[File]
    var plugins = new TreeSet[File]

    val tos = new TarOutputStream(file.bufferedOutputStream())
    try {
      for (moleJob ← job.moleJobs) moleJob.synchronized {
        if (!moleJob.finished) {
          val moleJobFile = Workspace.newFile("job", ".tar")
          try {
            val serializationResult =
              SerialiserService.serialiseGetPluginsAndFiles(
                RunnableTask(moleJob),
                moleJobFile)

            files ++= serializationResult.files
            plugins ++= serializationResult.plugins

            tos.addFile(moleJobFile, UUID.randomUUID.toString)
          }
          finally moleJobFile.delete
        }
      }
    }
    finally tos.close
    (files, plugins)
  }

  def toReplicatedFile(job: Job, file: File, storage: StorageService)(implicit token: AccessToken, session: Session): ReplicatedFile = {
    if (!file.exists) throw new UserBadDataError(s"File/category $file is required but doesn't exist.")

    val isDir = file.isDirectory
    var toReplicate = file
    val toReplicatePath = file.getAbsoluteFile

    //Hold cache to avoid gc and file deletion
    val cache = if (isDir) {
      val cache = FileService.archiveForDir(job.moleExecution, file)
      toReplicate = cache.file(false)
      cache
    }
    else null

    val hash = FileService.hash(job.moleExecution, toReplicate).toString
    val replica = ReplicaCatalog.uploadAndGet(toReplicate, toReplicatePath, hash, storage)
    ReplicatedFile(file, isDir, hash, replica.path, file.mode)
  }

  def replicateTheRuntime(
    job: Job,
    environment: BatchEnvironment,
    storage: StorageService)(implicit token: AccessToken, session: Session) = {

    val environmentPluginPath = environment.plugins.map { p ⇒ toReplicatedFile(job, p, storage) }.map { FileMessage(_) }
    val runtimeFileMessage = FileMessage(toReplicatedFile(job, environment.runtime, storage))
    val jvmLinuxI386FileMessage = FileMessage(toReplicatedFile(job, environment.jvmLinuxI386, storage))
    val jvmLinuxX64FileMessage = FileMessage(toReplicatedFile(job, environment.jvmLinuxX64, storage))

    val storageReplication = FileMessage(toReplicatedFile(job, storage.serializedRemoteStorage, storage))

    Runtime(
      storageReplication,
      runtimeFileMessage,
      environmentPluginPath,
      jvmLinuxI386FileMessage,
      jvmLinuxX64FileMessage)
  }

  def createExecutionMessage(
    job: Job,
    jobFile: File,
    serializationFile: Iterable[File],
    serializationPlugin: Iterable[File],
    storage: StorageService,
    path: String)(implicit token: AccessToken, session: Session): ExecutionMessage = {
    val jobForRuntimePath = storage.child(path, Storage.uniqName("job", ".tgz"))

    signalUpload(
      storage.uploadGZ(jobFile, jobForRuntimePath), jobForRuntimePath, storage)
    val jobHash = HashService.computeHash(jobFile).toString

    val pluginReplicas = serializationPlugin.map { toReplicatedFile(job, _, storage) }
    val files = serializationFile.map { toReplicatedFile(job, _, storage) }

    ExecutionMessage(
      pluginReplicas,
      files,
      FileMessage(jobForRuntimePath, jobHash),
      path)
  }

}
