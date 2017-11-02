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

import monocle.macros.Lenses
import org.openmole.core.context._
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.expansion.FromContext
import org.openmole.core.fileservice.FileService
import org.openmole.core.tools.service.OS
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.tools.InputOutputCheck
import org.openmole.core.workspace.NewFile
import org.openmole.tool.random._
import org.openmole.core.workflow.validation._

object External {
  val PWD = Val[String]("PWD")

  case class InputFile(
    prototype:   Val[File],
    destination: FromContext[String],
    link:        Boolean
  )

  case class InputFileArray(
    prototype: Val[Array[File]],
    prefix:    FromContext[String],
    suffix:    FromContext[String],
    link:      Boolean
  )

  case class OutputFile(
    origin:    FromContext[String],
    prototype: Val[File]
  )

  case class Resource(
    file:        File,
    destination: FromContext[String],
    link:        Boolean,
    os:          OS
  )

  case class ToPut(file: File, expandedUserPath: String, link: Boolean)
  type PathResolver = String ⇒ File

  def withWorkDir[T](executionContext: TaskExecutionContext)(f: File ⇒ T): T = {
    val tmpDir = executionContext.tmpDirectory.newDir("externalTask")
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

  def validate(external: External)(inputs: Seq[Val[_]]) = Validate { p ⇒
    import p._
    external.inputFileArrays.flatMap(_.prefix.validate(inputs)) ++
      external.inputFileArrays.flatMap(_.suffix.validate(inputs)) ++
      external.inputFiles.flatMap(_.destination.validate(inputs)) ++
      external.outputFiles.flatMap(_.origin.validate(inputs)) ++
      external.resources.flatMap(_.destination.validate(inputs))
  }
}

import org.openmole.plugin.task.external.External._

@Lenses case class External(
  inputFileArrays: Vector[External.InputFileArray] = Vector.empty,
  inputFiles:      Vector[External.InputFile]      = Vector.empty,
  outputFiles:     Vector[External.OutputFile]     = Vector.empty,
  resources:       Vector[External.Resource]       = Vector.empty
) {

  protected def listInputFiles(context: Context)(implicit rng: RandomProvider, newFile: NewFile, fileService: FileService): Vector[(Val[File], ToPut)] =
    inputFiles.map {
      case InputFile(prototype, name, link) ⇒ prototype → ToPut(context(prototype), name.from(context), link)
    }

  protected def listInputFileArray(context: Context)(implicit rng: RandomProvider, newFile: NewFile, fileService: FileService): Vector[(Val[Array[File]], Seq[ToPut])] =
    for {
      ifa ← inputFileArrays
    } yield {
      (
        ifa.prototype,
        context(ifa.prototype).zipWithIndex.map {
          case (file, i) ⇒
            ToPut(file, s"${ifa.prefix.from(context)}$i${ifa.suffix.from(context)}", link = ifa.link)
        }.toSeq
      )
    }

  protected def listResources(context: Context, resolver: PathResolver)(implicit rng: RandomProvider, newFile: NewFile, fileService: FileService): Iterable[ToPut] = {
    val byLocation =
      resources groupBy {
        case Resource(_, name, _, _) ⇒ resolver(name.from(context)).getCanonicalPath
      }

    val selectedOS =
      byLocation.toList flatMap {
        case (_, values) ⇒ values.find { _.os.compatible }
      }

    selectedOS.map {
      case Resource(file, name, link, _) ⇒ ToPut(file, name.from(context), link)
    }
  }

  def relativeResolver(workDirectory: File)(filePath: String): File = {
    def resolved = workDirectory.resolve(filePath)
    resolved.toFile
  }

  private def copyFile(f: ToPut, to: File) = {
    to.createParentDir

    if (f.link) to.createLinkTo(f.file.getCanonicalFile)
    else {
      f.file.realFile.copy(to)
      to.applyRecursive { _.deleteOnExit }
    }
  }

  def prepareInputFiles(context: Context, resolver: PathResolver)(implicit rng: RandomProvider, newFile: NewFile, fileService: FileService) =
    prepareAndListInputFiles(context, resolver)._1

  def prepareAndListInputFiles(context: Context, resolver: PathResolver)(implicit rng: RandomProvider, newFile: NewFile, fileService: FileService) = {
    def destination(f: ToPut) = resolver(f.expandedUserPath)

    val resourcesFiles = for { f ← listResources(context, resolver) } yield {
      val d = destination(f)
      copyFile(f, d)
      (f → d)
    }

    val (copiedFilesVariable, copiedFilesInfo) =
      listInputFiles(context).map {
        case (p, f) ⇒
          val d = destination(f)
          copyFile(f, d)
          (Variable(p, d), f → d)
      }.unzip

    val (copiedArrayFilesVariable, copiedFilesArrayInfo) =
      listInputFileArray(context).map {
        case (p, fs) ⇒
          val copied =
            fs.map { f ⇒
              val d = destination(f)
              copyFile(f, d)
              f → d
            }
          (Variable(p, copied.unzip._2.toArray), copied)
      }.unzip

    def allFileInfo = resourcesFiles ++ copiedFilesInfo ++ copiedFilesArrayInfo.flatten

    (context ++ copiedFilesVariable ++ copiedArrayFilesVariable, allFileInfo)
  }

  protected def outputFileVariables(context: Context, resolver: PathResolver)(implicit rng: RandomProvider, newFile: NewFile, fileService: FileService) =
    outputFiles.map {
      case OutputFile(name, prototype) ⇒
        val fileName = name.from(context)
        val file = resolver(fileName)
        Variable(prototype, file)
    }

  def contextFiles(outputs: PrototypeSet, context: Context): Seq[Variable[File]] =
    InputOutputCheck.filterOutput(outputs, context).values.filter { v ⇒ v.prototype.`type` == ValType[File] }.map(_.asInstanceOf[Variable[File]]).toSeq

  def fetchOutputFiles(outputs: PrototypeSet, context: Context, resolver: PathResolver, workDirectory: File)(implicit rng: RandomProvider, newFile: NewFile, fileService: FileService): Context = {
    val fileOutputs = outputFileVariables(context, resolver)
    val allFiles = (fileOutputs ++ contextFiles(outputs, context)).distinct

    for {
      f ← allFiles
      if !f.value.exists
    } throw new UserBadDataError("Output file " + f.value.getAbsolutePath + s" (stored in variable ${f.prototype}) doesn't exist, parent directory ${f.value.getParentFileSafe} contains [" + f.value.getParentFileSafe.listFilesSafe.map(_.getName).mkString(", ") + "]")

    // If the file path contains a symbolic link, the link might be deleted by the cleaning operation
    context ++ allFiles.map { v ⇒
      if (workDirectory.isAParentOf(v.value)) Variable.copy(v)(value = v.value.realFile) else v
    }
  }

  def cleanWorkDirectory(outputs: PrototypeSet, context: Context, workDirectory: File)(implicit fileService: FileService) = {
    val ctxFiles = contextFiles(outputs, context)

    for {
      f ← ctxFiles
      if workDirectory.isAParentOf(f.value)
    } fileService.deleteWhenGarbageCollected(f.value)

    workDirectory.applyRecursive(f ⇒ f.delete, ctxFiles.map(_.value))
    workDirectory.applyRecursive(f ⇒ if (f.isDirectory) fileService.deleteWhenEmpty(f), ctxFiles.map(_.value))
  }

}
