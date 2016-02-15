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
import org.openmole.tool.file._
import org.openmole.core.tools.service.OS
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.tools.ExpandedString
import org.openmole.core.workspace.Workspace

import scala.util.Random

object ExternalTask {
  val PWD = Prototype[String]("PWD")

  case class InputFile(
    prototype: Prototype[File],
    destination: ExpandedString,
    link: Boolean,
    toWorkDirectory: Boolean)

  case class InputFileArray(
    prototype: Prototype[Array[File]],
    prefix: ExpandedString,
    suffix: ExpandedString,
    link: Boolean,
    toWorkDirectory: Boolean)

  case class OutputFile(
    origin: ExpandedString,
    prototype: Prototype[File],
    fromWorkDirectory: Boolean)

  case class Resource(
    file: File,
    destination: ExpandedString,
    link: Boolean,
    toWorkDirectory: Boolean,
    os: OS)
}

import ExternalTask._

trait ExternalTask extends Task {

  def inputFileArrays: Iterable[InputFileArray]
  def inputFiles: Iterable[InputFile]
  def outputFiles: Iterable[OutputFile]
  def resources: Iterable[Resource]

  protected case class ToPut(file: File, name: String, link: Boolean, inWorkDir: Boolean)

  protected def listInputFiles(context: Context)(implicit rng: RandomProvider): Iterable[(Prototype[File], ToPut)] =
    inputFiles.map {
      case InputFile(prototype, name, link, toWorkDir) ⇒ prototype -> ToPut(context(prototype), name.from(context), link, toWorkDir)
    }

  protected def listInputFileArray(context: Context)(implicit rng: RandomProvider): Iterable[(Prototype[Array[File]], Seq[ToPut])] =
    for {
      ifa ← inputFileArrays
    } yield {
      (ifa.prototype,
        context(ifa.prototype).zipWithIndex.map {
          case (file, i) ⇒
            ToPut(file, s"${ifa.prefix.from(context)}$i${ifa.suffix.from(context)}", link = ifa.link, inWorkDir = ifa.toWorkDirectory)
        }.toSeq)
    }

  protected def listResources(context: Context, tmpDir: File)(implicit rng: RandomProvider): Iterable[ToPut] = {
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

  protected def outputFileVariables(context: Context, tmpDir: File, workDirPath: Option[String])(implicit rng: RandomProvider) = {
    val workDir = workDirPath.map(new File(tmpDir, _)).getOrElse(tmpDir)

    outputFiles.map {
      case OutputFile(name, prototype, inWorkDir) ⇒
        val fileName = name.from(context)
        val baseDir = if (inWorkDir) workDir else tmpDir
        Variable(prototype, baseDir.resolve(fileName).toFile)
    }
  }

  private def copy(f: ToPut, to: File) = {
    to.createParentDir

    if (f.link) to.createLink(f.file.getCanonicalFile)
    else {
      f.file.realFile.copy(to)
      to.applyRecursive { _.deleteOnExit }
    }

  }

  def prepareInputFiles(context: Context, tmpDir: File, workDirPath: Option[String])(implicit rng: RandomProvider): Context = {
    val workDir = workDirPath.map(new File(tmpDir, _)).getOrElse(tmpDir)
    workDir.mkdirs()
    def destination(f: ToPut) = if (f.inWorkDir) new File(workDir, f.name) else new File(tmpDir, f.name)

    for { f ← listResources(context, tmpDir) } copy(f, destination(f))

    val copiedFiles =
      for { (p, f) ← listInputFiles(context) } yield {
        val d = destination(f)
        copy(f, d)
        Variable(p, d)
      }

    val copiedArrayFiles =
      for { (p, fs) ← listInputFileArray(context) } yield {
        val copied =
          fs.map { f ⇒
            val d = destination(f)
            copy(f, d)
            d
          }
        Variable(p, copied.toArray)
      }

    context ++ copiedFiles ++ copiedArrayFiles
  }

  def fetchOutputFiles(context: Context, tmpDir: File, workDirPath: Option[String])(implicit rng: RandomProvider): Context = {
    val resultContext = context ++ outputFileVariables(context, tmpDir, workDirPath)

    def contextFiles =
      filterOutput(resultContext).values.map(_.value).collect { case f: File ⇒ f }

    for {
      f ← contextFiles
      if !f.exists
    } throw new UserBadDataError("Output file " + f.getAbsolutePath + " for task " + this.toString + " doesn't exist")

    tmpDir.applyRecursive(f ⇒ f.delete, contextFiles.toSet)

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
