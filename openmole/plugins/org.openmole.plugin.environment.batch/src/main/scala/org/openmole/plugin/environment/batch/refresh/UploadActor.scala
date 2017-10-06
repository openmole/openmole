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

package org.openmole.plugin.environment.batch.refresh

import java.io.File
import java.util.UUID

import org.openmole.core.communication.message._
import org.openmole.core.communication.storage._
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.fileservice.FileService
import org.openmole.core.serializer._
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.workflow.job._
import org.openmole.core.workspace.{ NewFile, Workspace }
import org.openmole.plugin.environment.batch.control._
import org.openmole.plugin.environment.batch.environment.BatchEnvironment.signalUpload
import org.openmole.plugin.environment.batch.environment._
import org.openmole.plugin.environment.batch.storage._
import org.openmole.tool.file.{ uniqName, _ }
import org.openmole.tool.logger.Logger
import org.openmole.tool.random._

import scala.collection.immutable.TreeSet

object UploadActor extends Logger {

  def receive(msg: Upload)(implicit services: BatchEnvironment.Services) = {
    import services._

    val job = msg.job
    if (!job.state.isFinal) {
      try job.environment.trySelectStorage(jobFiles(job)) match {
        case Some((storage, token)) ⇒
          try {
            implicit val implicitToken = token
            val sj = initCommunication(job, storage)
            JobManager ! Uploaded(job, sj)
          }
          finally BatchService.releaseToken(storage.usageControl, token)
        case None ⇒ JobManager ! Delay(msg, BatchEnvironment.getTokenInterval)
      }
      catch {
        case e: Throwable ⇒
          JobManager ! Error(job, e, None)
          JobManager ! msg
      }
    }
  }

  private def jobFiles(job: BatchExecutionJob) =
    job.pluginsAndFiles.files.toVector ++
      job.pluginsAndFiles.plugins ++
      job.environment.plugins ++
      Seq(job.environment.jvmLinuxX64, job.environment.runtime)

  private def initCommunication(job: BatchExecutionJob, storage: StorageService[_])(implicit token: AccessToken, services: BatchEnvironment.Services): SerializedJob = services.newFile.withTmpFile("job", ".tar") { jobFile ⇒
    import services._

    serializerService.serialise(job.runnableTasks, jobFile)

    val plugins = new TreeSet[File]()(fileOrdering) ++ job.plugins
    val files = (new TreeSet[File]()(fileOrdering) ++ job.files) diff plugins

    val communicationPath = storage.child(storage.tmpDir, UUID.randomUUID.toString)
    storage.makeDir(communicationPath)

    val inputPath = storage.child(communicationPath, uniqName("job", ".in"))

    val runtime = replicateTheRuntime(job.job, job.environment, storage)

    val executionMessage = createExecutionMessage(
      job.job,
      jobFile,
      files,
      plugins,
      storage,
      communicationPath
    )

    /* ---- upload the execution message ----*/
    newFile.withTmpFile("job", ".tar") { executionMessageFile ⇒
      serializerService.serialiseAndArchiveFiles(executionMessage, executionMessageFile)
      signalUpload(eventDispatcher.eventId, storage.upload(executionMessageFile, inputPath, TransferOptions(forceCopy = true, canMove = true)), executionMessageFile, inputPath, storage)
    }

    SerializedJob(storage, communicationPath, inputPath, runtime)
  }

  def toReplicatedFile(file: File, storage: StorageService[_], transferOptions: TransferOptions)(implicit token: AccessToken, services: BatchEnvironment.Services): ReplicatedFile = {
    import services._

    if (!file.exists) throw new UserBadDataError(s"File $file is required but doesn't exist.")

    val isDir = file.isDirectory
    val toReplicatePath = file.getCanonicalFile

    val (toReplicate, options) =
      if (isDir) (services.fileService.archiveForDir(file).file, transferOptions.copy(forceCopy = true))
      else (file, transferOptions)

    val fileMode = file.mode
    val hash = services.fileService.hash(toReplicate).toString

    def upload = {
      val name = uniqName(System.currentTimeMillis.toString, ".rep")
      val newFile = storage.child(storage.persistentDir, name)
      Log.logger.fine(s"Upload $toReplicate to $newFile on ${storage.id} mode $fileMode")
      signalUpload(eventDispatcher.eventId, storage.upload(toReplicate, newFile, options), toReplicate, newFile, storage)
      newFile
    }

    val replica = services.replicaCatalog.uploadAndGet(upload, toReplicatePath, hash, storage)
    ReplicatedFile(file.getPath, isDir, hash, replica.path, fileMode)
  }

  def replicateTheRuntime(
    job:         Job,
    environment: BatchEnvironment,
    storage:     StorageService[_]
  )(implicit token: AccessToken, services: BatchEnvironment.Services) = {
    val environmentPluginPath = shuffled(environment.plugins)(services.randomProvider()).map { p ⇒ toReplicatedFile(p, storage, TransferOptions(raw = true)) }.map { FileMessage(_) }
    val runtimeFileMessage = FileMessage(toReplicatedFile(environment.runtime, storage, TransferOptions(raw = true)))
    val jvmLinuxX64FileMessage = FileMessage(toReplicatedFile(environment.jvmLinuxX64, storage, TransferOptions(raw = true)))

    val storageReplication = FileMessage(toReplicatedFile(storage.serializedRemoteStorage, storage, TransferOptions(raw = true, forceCopy = true)))

    Runtime(
      storageReplication,
      runtimeFileMessage,
      environmentPluginPath.toSet,
      jvmLinuxX64FileMessage
    )
  }

  def createExecutionMessage(
    job:                 Job,
    jobFile:             File,
    serializationFile:   Iterable[File],
    serializationPlugin: Iterable[File],
    storage:             StorageService[_],
    path:                String
  )(implicit token: AccessToken, services: BatchEnvironment.Services): ExecutionMessage = {

    val pluginReplicas = shuffled(serializationPlugin)(services.randomProvider()).map { toReplicatedFile(_, storage, TransferOptions(raw = true)) }
    val files = shuffled(serializationFile)(services.randomProvider()).map { toReplicatedFile(_, storage, TransferOptions()) }

    ExecutionMessage(
      pluginReplicas,
      files,
      jobFile,
      path,
      storage.environment.runtimeSettings
    )
  }

}
