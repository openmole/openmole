/*
 *  Copyright (C) 2010 Romain Reuillon <romain.Romain Reuillon at openmole.org>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 * 
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.task.external

import java.io.File
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.tools.io.FileUtil
import org.openmole.core.tools.service.OS
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.task.Task
import org.openmole.core.workflow.tools.ExpandedString
import org.openmole.core.workspace.Workspace
import scala.collection.mutable.ListBuffer
import FileUtil._
import collection.mutable

object ExternalTask {
  val PWD = Prototype[String]("PWD")

  case class InputFile(
    prototype: Prototype[File],
    destination: ExpandedString,
    link: Boolean,
    inWorkDir: Boolean)

  case class OutputFile(
    origin: ExpandedString,
    prototype: Prototype[File],
    inWorkDir: Boolean)

  case class Resource(
    file: File,
    destination: ExpandedString,
    link: Boolean,
    toWorkDir: Boolean,
    os: OS)
}

import ExternalTask._

trait ExternalTask extends Task {

  def inputFiles: Iterable[InputFile]
  def outputFiles: Iterable[OutputFile]
  def resources: Iterable[Resource]

  protected case class ToPut(file: File, name: String, link: Boolean, inWorkDir: Boolean)
  protected case class ToGet(name: String, file: File, inWorkDir: Boolean)

  protected def listInputFiles(context: Context): Iterable[ToPut] =
    inputFiles.map {
      case InputFile(prototype, name, link, toWorkDir) ⇒ ToPut(context(prototype), name.from(context), link, toWorkDir)
    }

  protected def listResources(context: Context, tmpDir: File): Iterable[ToPut] = {
    val byLocation =
      resources groupBy {
        case Resource(_, name, _, _, _) ⇒ new File(tmpDir, name.from(context)).getCanonicalPath
      }

    val selectedOS =
      byLocation.toList flatMap {
        case (_, values) ⇒ values.find { _.os.compatible }
      }

    selectedOS.map {
      case Resource(file, name, link, toWorkDir, _) ⇒ ToPut(file, name.from(context), link, toWorkDir)
    }
  }

  protected def listOutputFiles(context: Context, tmpDir: File, workDirPath: String): (Context, Iterable[ToGet]) = {
    val workDir = new File(tmpDir, workDirPath)

    val files =
      outputFiles.map {
        case OutputFile(name, prototype, inWorkDir) ⇒
          val fileName = name.from(context)
          val file = if (inWorkDir) new File(workDir, fileName) else new File(tmpDir, fileName)

          val fileVariable = Variable(prototype, file)
          ToGet(fileName, file, inWorkDir) -> fileVariable
      }
    context ++ files.map { _._2 } -> files.map { _._1 }
  }

  private def copy(f: ToPut, to: File) = {
    to.getAbsoluteFile.getParentFile.mkdirs

    if (f.link) to.createLink(f.file.getAbsolutePath)
    else {
      f.file.copy(to)
      to.applyRecursive { _.deleteOnExit }
    }
  }

  def prepareInputFiles(context: Context, tmpDir: File, workDirPath: String) = {
    val workDir = new File(tmpDir, workDirPath)
    def destination(f: ToPut) = if (f.inWorkDir) new File(workDir, f.name) else new File(tmpDir, f.name)

    for { f ← listResources(context, tmpDir) } copy(f, destination(f))
    for { f ← listInputFiles(context) } copy(f, destination(f))
  }

  def fetchOutputFiles(context: Context, tmpDir: File, workDirPath: String): Context = {
    val (resultContext, outputFiles) = listOutputFiles(context, tmpDir, workDirPath: String)

    val usedFiles = outputFiles.map(
      f ⇒ {
        if (!f.file.exists) throw new UserBadDataError("Output file " + f.file.getAbsolutePath + " for task " + this.toString + " doesn't exist")
        f.file
      }).toSet

    tmpDir.applyRecursive(f ⇒ f.delete, usedFiles)

    // This delete the dir only if it is empty
    tmpDir.delete
    resultContext
  }

  def withWorkDir[T](f: File ⇒ T): T = {
    val tmpDir = Workspace.newDir("externalTask")
    val res =
      try f(tmpDir)
      catch {
        case e: Throwable ⇒
          tmpDir.recursiveDelete
          throw e
      }
    tmpDir.delete
    res
  }

}
