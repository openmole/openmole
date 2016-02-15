/*
 * Copyright (C) 2015 Romain Reuillon
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
package org.openmole.core.batch

import java.io.File

import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.fileservice.FileDeleter
import org.openmole.core.tools.service._
import org.openmole.core.workflow.data.Context
import org.openmole.core.workflow.execution.Environment.{ RuntimeLog }
import org.openmole.core.workflow.job.MoleJob._
import org.openmole.core.workflow.job._
import org.openmole.core.workflow.task.Task
import org.openmole.core.workspace.Workspace
import util.Try
import org.openmole.tool.file._
import org.openmole.tool.hash._
import org.openmole.tool.tar._

package object message {

  object FileMessage {
    implicit def replicatedFile2FileMessage(r: ReplicatedFile) = FileMessage(r)
    def apply(replicatedFile: ReplicatedFile): FileMessage = apply(replicatedFile.path, replicatedFile.hash)
  }
  implicit class ReplicatedFileDecorator(replicatedFile: ReplicatedFile) {
    def download(download: (String, File) ⇒ Unit, verifyHash: Boolean = false) = {
      val cache = Workspace.newFile()

      download(replicatedFile.path, cache)

      if (verifyHash) {
        val cacheHash = cache.hash.toString
        if (cacheHash != replicatedFile.hash) throw new InternalProcessingError("Hash is incorrect for file " + replicatedFile.originalPath + " replicated at " + replicatedFile.path)
      }

      val dl =
        if (replicatedFile.directory) {
          val local = Workspace.newDir("dirReplica")
          cache.extract(local)
          local.mode = replicatedFile.mode
          cache.delete
          local
        }
        else {
          cache.mode = replicatedFile.mode
          cache
        }

      dl
    }
  }

  implicit class FileToReplicatedFileDecorator(file: File) {
    def upload(upload: File ⇒ String) = {
      val isDir = file.isDirectory

      val toReplicate =
        if (isDir) {
          val ret = Workspace.newFile("archive", ".tar")
          file.archive(ret)
          ret
        }
        else file

      val mode = file.mode
      val hash = toReplicate.hash.toString
      val newFile = upload(toReplicate)
      ReplicatedFile(file.getPath, isDir, hash, newFile, mode)
    }
  }

  object RunnableTask {
    def apply(moleJob: MoleJob) = new RunnableTask(moleJob.task, moleJob.context, moleJob.id)
  }

  class RunnableTask(val task: Task, val context: Context, val id: MoleJobId) {
    def toMoleJob(stateChangedCallBack: StateChangedCallBack) = MoleJob(task, context, id, stateChangedCallBack)
  }

  case class FileMessage(path: String, hash: String)

  case class ReplicatedFile(originalPath: String, directory: Boolean, hash: String, path: String, mode: Int)
  case class RuntimeSettings(archiveResult: Boolean)
  case class ExecutionMessage(plugins: Iterable[ReplicatedFile], files: Iterable[ReplicatedFile], jobs: File, communicationDirPath: String, runtimeSettings: RuntimeSettings) {
    FileDeleter.deleteWhenGarbageCollected(jobs)
  }

  sealed trait SerializedContextResults

  case class ArchiveContextResults(contextResults: File) extends SerializedContextResults {
    FileDeleter.deleteWhenGarbageCollected(contextResults)
  }

  case class IndividualFilesContextResults(contextResults: File, files: Iterable[ReplicatedFile]) extends SerializedContextResults {
    FileDeleter.deleteWhenGarbageCollected(contextResults)
  }
  case class ContextResults(results: PartialFunction[MoleJobId, Try[Context]])

  case class RuntimeResult(stdOut: Option[File], stdErr: Option[File], result: Try[(SerializedContextResults, RuntimeLog)], info: RuntimeInfo) {
    stdOut.foreach(FileDeleter.deleteWhenGarbageCollected)
    stdErr.foreach(FileDeleter.deleteWhenGarbageCollected)
  }

}
