/*
 * Copyright (C) 2011 romain
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

package org.openmole.plugin.instantrerun.filesystem

import org.openmole.core.model.data.Context
import java.io.FileOutputStream
import java.io.FileInputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.io.File
import org.openmole.misc.exception._
import org.openmole.core.model.task._
import org.openmole.core.model.job.IMoleJob._
import org.openmole.core.model.job._
import org.openmole.core.model.mole._
import org.openmole.core.model.data._
import org.openmole.core.implementation.data._
import org.openmole.core.serializer._
import org.openmole.core.serializer.structure.FileInfo
import org.openmole.misc.hashservice.HashService._
import org.openmole.misc.tools.service._
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.tools.io.FileUtil._
import scala.collection.immutable.TreeMap
import scala.collection.immutable.TreeSet
import scala.collection.mutable.HashMap
import scala.io.Source

object FileSystemInstantRerun {
  val FILE = "file"
  val CONTEXT = "context"
}

class FileSystemInstantRerun(dir: File, capsules: Set[ICapsule]) extends IInstantRerun {

  def this(dir: File, capsules: Array[ICapsule]) = this(dir, capsules.toSet)
  def this(dir: String, capsules: Array[ICapsule]) = this(new File(dir), capsules)
  def this(dir: File, capsules: ICapsule*) = this(dir, capsules.toSet)
  def this(dir: String, capsules: ICapsule*) = this(new File(dir), capsules.toSet)

  import FileSystemInstantRerun._

  @transient lazy val fileDir = {
    val ret = new File(dir, FILE)
    ret.mkdirs
    ret
  }

  @transient lazy val contextDir = {
    val ret = new File(dir, CONTEXT)
    ret.mkdirs
    ret
  }

  private var jobsInProgressHash = new TreeMap[MoleJobId, (Hash, Hash)]

  def rerun(job: IMoleJob, capsule: ICapsule): Boolean = synchronized {
    if (!capsules.contains(capsule)) return false

    val taskHash = hashTask(capsule.task)
    val taskDir = new File(contextDir, taskHash.toString)

    val serializedContext = saveContext(job)
    try {
      val contextHash = computeHash(serializedContext._1)
      val contextDir = new File(taskDir, contextHash.toString)

      if (!contextDir.exists) {
        jobsInProgressHash += job.id -> ((taskHash, contextHash))
        false
      } else {
        val is = new GZIPInputStream(new FileInputStream(contextDir.listFiles()(0)))
        val context = try SerializerService.deserializeReplacePathHash[Context](is,
          new PartialFunction[FileInfo, File] {
            val allReadyCopied = new HashMap[FileInfo, File]

            override def apply(fileInfo: FileInfo) = allReadyCopied.synchronized {
              allReadyCopied.getOrElseUpdate(fileInfo, {
                val file = new File(fileDir, fileInfo.fileHash.toString)

                if (fileInfo.isDir) {
                  val dest = Workspace.newDir
                  file.extractUncompressDirArchiveWithRelativePath(dest)
                  dest
                } else {
                  val dest = Workspace.newFile("file", ".bin")
                  file.copyUncompressFile(dest)
                  dest
                }
              })
            }

            override def isDefinedAt(fileInfo: FileInfo) = new File(fileDir, fileInfo.fileHash.toString).exists
          })
        finally is.close

        job.finish(context, Seq.empty)
        true
      }

    } finally serializedContext._1.delete
  }

  def jobFinished(job: IMoleJob, capsule: ICapsule) = synchronized {
    if (job.state == State.COMPLETED && capsules.contains(capsule)) {
      jobsInProgressHash.get(job.id) match {
        case Some((taskH, contextH)) ⇒
          val contextFile = saveContext(job)
          try {
            val files = contextFile._2

            //Should take dir into account, where are stored the isdir metadata?
            for (f ← files) {
              val file = new File(fileDir, f._2.fileHash.toString)
              if (!file.exists) {
                if (f._1.isDirectory) f._1.archiveCompressDirWithRelativePathNoVariableContent(file)
                else f._1.copyCompressFile(file)
              }
            }

            val taskDir = new File(contextDir, taskH.toString)
            val destDir = new File(taskDir, contextH.toString)
            //println(destDir.getAbsolutePath)
            destDir.mkdirs

            //println("Create " + destDir.getAbsolutePath)

            val dest = new File(destDir, computeHash(contextFile._1).toString)
            if (!dest.exists) {
              if (!destDir.list.isEmpty) throw new UserBadDataError("Using instant rerun on task with non-deterministic behavior.")
              contextFile._1.move(dest)
            }

          } finally contextFile._1.delete
        case None ⇒
      }
      jobsInProgressHash -= job.id
    }
  }

  private def saveContext(moleJob: IMoleJob) = {
    val file = Workspace.newFile("context", ".xml")
    val accepted = TreeSet.empty[String] ++ moleJob.task.outputs.map(_.prototype.name)
    val context = Context(moleJob.context.values.filter(v ⇒ accepted.contains(v.prototype.name)))
    val os = new GZIPOutputStream(new FileOutputStream(file))
    (file, try SerializerService.serializeFilePathAsHashGetFiles(context, os) finally os.close)
  }

  private def hashTask(task: ITask) = {
    val file = Workspace.newFile("context", ".xml")
    try {
      SerializerService.serializeFilePathAsHashGetFiles(task, file)
      computeHash(file)
    } finally file.delete
  }

}
