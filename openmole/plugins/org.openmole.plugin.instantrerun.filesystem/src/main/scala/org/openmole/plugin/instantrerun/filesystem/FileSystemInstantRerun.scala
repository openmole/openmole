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

import org.openmole.core.model.data.IContext
import java.io.FileOutputStream
import java.io.FileInputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.io.File
import org.openmole.misc.exception.UserBadDataError
import org.openmole.core.model.task.IGenericTask
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.job.IMoleJob._
import org.openmole.core.model.job.MoleJobId
import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.core.model.mole.IInstantRerun
import org.openmole.core.model.data.IContext
import org.openmole.core.implementation.data.Context
import org.openmole.core.serializer.SerializerService
import org.openmole.core.serializer.structure.FileInfo
import org.openmole.misc.hashservice.HashService._
import org.openmole.misc.tools.service.IHash
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.core.implementation.task.GenericTask.Timestamps
import scala.collection.immutable.TreeMap
import scala.collection.immutable.TreeSet
import scala.collection.mutable.HashMap
import scala.io.Source

object FileSystemInstantRerun {
  val FILE = "file"
  val CONTEXT = "context"
}

class FileSystemInstantRerun(dir: File, capsules: Set[IGenericCapsule]) extends IInstantRerun {

  def this(dir: File, capsules: Array[IGenericCapsule]) = this(dir, capsules.toSet)
  def this(dir: String, capsules: Array[IGenericCapsule]) = this(new File(dir), capsules)
  def this(dir: File, capsules: IGenericCapsule*) = this(dir, capsules.toSet)
  def this(dir: String, capsules: IGenericCapsule*) = this(new File(dir), capsules.toSet)
  
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
  
  private var jobsInProgressHash = new TreeMap[MoleJobId, (IHash, IHash)]
  
  def rerun(job: IMoleJob, capsule: IGenericCapsule): Boolean = synchronized {
    if(!capsules.contains(capsule)) return false
    
    val taskHash = hashTask(capsule.taskOrException)
    val taskDir = new File(contextDir, taskHash.toString)  

    val serializedContext = saveContext(job)
    try {
      val contextHash = computeHash(serializedContext._1)
      val contextDir = new File(taskDir, contextHash.toString)
      
      //println("Test " + contextDir.getAbsolutePath)
      
      if(!contextDir.exists) {
        jobsInProgressHash += job.id -> ((taskHash, contextHash))
        false
      } else {
        val is = new GZIPInputStream(new FileInputStream(contextDir.listFiles()(0)))
        val context = try SerializerService.deserializeReplacePathHash[IContext](is,
                                                     new PartialFunction[FileInfo, File] {
            val allReadyCopied = new HashMap[FileInfo, File]
            
            override def apply(fileInfo: FileInfo) = allReadyCopied.synchronized {
              allReadyCopied.getOrElseUpdate(fileInfo,{
                  val file = new File(fileDir, fileInfo.fileHash.toString)
                  
                  if(fileInfo.isDir) {
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
        job.task.outputs.toList.filter(_.mode.isSystem).map{d => job.context.variable(d.prototype)}.foreach {
          variable => variable match {
            case Some(v) => context += v
            case None =>
          }
        }

        job.finished(context)
        true
      }
      
    } finally serializedContext._1.delete
  }
  
  def jobFinished(job: IMoleJob, capsule: IGenericCapsule) = synchronized {
    if(capsules.contains(capsule)) {
      jobsInProgressHash.get(job.id) match {
        case Some((taskH, contextH)) =>
          val contextFile = saveContext(job)
          try {
            val files = contextFile._2
    
            //Should take dir into account, where are stored the isdir metadata?
            for (f <- files) { 
              val file = new File(fileDir, f._2.fileHash.toString)
              if(!file.exists) {
                if(f._1.isDirectory) f._1.archiveCompressDirWithRelativePathNoVariableContent(file)
                else f._1.copyCompressFile(file)
              }
            }
    
            val taskDir = new File(contextDir, taskH.toString)
            val destDir = new File(taskDir, contextH.toString)
            //println(destDir.getAbsolutePath)
            destDir.mkdirs
    
            //println("Create " + destDir.getAbsolutePath)
 
            
            val dest = new File(destDir, computeHash(contextFile._1).toString)
            if(!dest.exists) {
              if(!destDir.list.isEmpty) throw new UserBadDataError("Using instant rerun on task with non-deterministic behavior.")
              contextFile._1.move(dest)
            }

          } finally contextFile._1.delete
        case None =>
      }
      jobsInProgressHash -= job.id
    }
  }
  
  private def saveContext(moleJob: IMoleJob) = {
    val file = Workspace.newFile("context", ".xml")
    val accepted = TreeSet.empty[String] ++ moleJob.task.userOutputs.map(_.prototype.name)
    val context = Context(moleJob.context.filter(v => accepted.contains(v.prototype.name)))
    val os = new GZIPOutputStream(new FileOutputStream(file))
    (file, try SerializerService.serializeFilePathAsHashGetFiles(context, os) finally os.close)
  }
  
  private def hashTask(task: IGenericTask) = {
    val file = Workspace.newFile("context", ".xml")
    try {
      SerializerService.serializeFilePathAsHashGetFiles(task, file)
      computeHash(file)
    } finally file.delete
  }
  
}
