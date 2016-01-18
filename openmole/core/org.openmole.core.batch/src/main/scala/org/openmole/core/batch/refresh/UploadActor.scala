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
import org.openmole.tool.file._
import org.openmole.tool.hash._
import org.openmole.core.workflow.job._

import org.openmole.core.serializer._
import org.openmole.core.workspace.Workspace
import org.openmole.tool.logger.Logger
import org.openmole.tool.tar.TarOutputStream
import scala.collection.immutable.TreeSet

object UploadActor extends Logger

import UploadActor._

class UploadActor(jobManager: JobManager) {

  def receive(msg: Upload) = withRunFinalization {
    val job = msg.job
    if (!job.state.isFinal) {
      try job.trySelectStorage match {
        case Some((storage, token)) ⇒
          try {
            val sj = initCommunication(job, storage)(token)
            jobManager ! Uploaded(job, sj)
          }
          finally storage.releaseToken(token)
        case None ⇒ jobManager ! Delay(msg, BatchEnvironment.getTokenInterval)
      }
      catch {
        case e: Throwable ⇒
          jobManager ! Error(job, e)
          jobManager ! msg
      }
    }
  }

  private def initCommunication(job: BatchExecutionJob, storage: StorageService)(implicit token: AccessToken): SerializedJob = Workspace.withTmpFile("job", ".tar") { jobFile ⇒
    SerialiserService.serialise(job.runnableTasks, jobFile)

    val pluginAndFiles = job.pluginsAndFiles

    val plugins = new TreeSet[File]()(fileOrdering) ++ pluginAndFiles.plugins
    val files = (new TreeSet[File]()(fileOrdering) ++ pluginAndFiles.files) diff plugins

    val communicationPath = storage.child(storage.tmpDir, UUID.randomUUID.toString)
    storage.makeDir(communicationPath)

    val inputPath = storage.child(communicationPath, Storage.uniqName("job", ".in"))

    val runtime = replicateTheRuntime(job.job, job.environment, storage)

    val jobForRuntimePath = storage.child(communicationPath, Storage.uniqName("job", ".tgz"))

    val jobHash = jobFile.hash.toString
    signalUpload(storage.upload(jobFile, jobForRuntimePath, TransferOptions(forceCopy = true, canMove = true)), jobFile, jobForRuntimePath, storage)
    val jobMessage = FileMessage(jobForRuntimePath, jobHash)

    val executionMessage = createExecutionMessage(
      job.job,
      jobMessage,
      files,
      plugins,
      storage,
      communicationPath)

    /* ---- upload the execution message ----*/
    Workspace.withTmpFile("job", ".xml") { executionMessageFile ⇒
      SerialiserService.serialise(executionMessage, executionMessageFile)
      signalUpload(storage.upload(executionMessageFile, inputPath, TransferOptions(forceCopy = true, canMove = true)), executionMessageFile, inputPath, storage)
    }

    SerializedJob(storage, communicationPath, inputPath, runtime)
  }

  def toReplicatedFile(job: Job, file: File, storage: StorageService, transferOptions: TransferOptions)(implicit token: AccessToken): ReplicatedFile = {
    if (!file.exists) throw new UserBadDataError(s"File $file is required but doesn't exist.")

    val isDir = file.isDirectory
    val toReplicatePath = file.getCanonicalFile

    val (toReplicate, options) =
      if (isDir) (FileService.archiveForDir(job.moleExecution, file).file, transferOptions.copy(forceCopy = true))
      else (file, transferOptions)

    val fileMode = file.mode
    val hash = FileService.hash(job.moleExecution, toReplicate).toString

    def upload = {
      val name = Storage.uniqName(System.currentTimeMillis.toString, ".rep")
      val newFile = storage.child(storage.persistentDir, name)
      Log.logger.fine(s"Upload $toReplicate to $newFile on ${storage.id} mode $fileMode")
      signalUpload(storage.upload(toReplicate, newFile, options), toReplicate, newFile, storage)
      newFile
    }

    val replica = ReplicaCatalog.uploadAndGet(upload, toReplicatePath, hash, storage)
    ReplicatedFile(file.getPath, isDir, hash, replica.path, fileMode)
  }

  def replicateTheRuntime(
    job: Job,
    environment: BatchEnvironment,
    storage: StorageService)(implicit token: AccessToken) = {

    val environmentPluginPath = environment.plugins.map { p ⇒ toReplicatedFile(job, p, storage, TransferOptions(raw = true)) }.map { FileMessage(_) }
    val runtimeFileMessage = FileMessage(toReplicatedFile(job, environment.runtime, storage, TransferOptions(raw = true)))
    val jvmLinuxI386FileMessage = FileMessage(toReplicatedFile(job, environment.jvmLinuxI386, storage, TransferOptions(raw = true)))
    val jvmLinuxX64FileMessage = FileMessage(toReplicatedFile(job, environment.jvmLinuxX64, storage, TransferOptions(raw = true)))

    val storageReplication = FileMessage(toReplicatedFile(job, storage.serializedRemoteStorage, storage, TransferOptions(raw = true, forceCopy = true)))

    Runtime(
      storageReplication,
      runtimeFileMessage,
      environmentPluginPath,
      jvmLinuxI386FileMessage,
      jvmLinuxX64FileMessage)
  }

  def createExecutionMessage(
    job: Job,
    jobMessage: FileMessage,
    serializationFile: Iterable[File],
    serializationPlugin: Iterable[File],
    storage: StorageService,
    path: String)(implicit token: AccessToken): ExecutionMessage = {

    val pluginReplicas = serializationPlugin.map { toReplicatedFile(job, _, storage, TransferOptions(raw = true)) }
    val files = serializationFile.map { toReplicatedFile(job, _, storage, TransferOptions()) }

    ExecutionMessage(
      pluginReplicas,
      files,
      jobMessage,
      path,
      storage.environment.runtimeSettings
    )
  }

}
