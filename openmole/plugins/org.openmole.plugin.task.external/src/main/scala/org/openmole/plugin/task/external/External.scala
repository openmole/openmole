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

import monocle.macros.Lenses
import org.openmole.core.tools.service.OS
import org.openmole.core.dsl._
import org.openmole.core.dsl.`extension`._
import org.openmole.core.workflow.tools.InputOutputCheck

object External {
  val PWD = Val[String]("PWD")

  trait ToInputFile[T] {
    def apply(t: T): InputFile
  }

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

  sealed trait DeployedFileType

  object DeployedFileType {
    case object InputFile extends DeployedFileType
    case object Resource extends DeployedFileType
  }

  case class DeployedFile(file: File, expandedUserPath: String, link: Boolean, deployedFileType: DeployedFileType)
  type PathResolver = String ⇒ File

  def validate(external: External)(inputs: Seq[Val[_]]): Validate = {
    def resourceExists(resource: External.Resource) = Validate {
      if (!resource.file.exists()) Seq(new UserBadDataError(s"""File resource "${resource.file} doesn't exist.""")) else Seq.empty
    }

    external.inputFileArrays.flatMap(_.prefix.validate(inputs)) ++
      external.inputFileArrays.flatMap(_.suffix.validate(inputs)) ++
      external.inputFiles.flatMap(_.destination.validate(inputs)) ++
      external.outputFiles.flatMap(_.origin.validate(inputs)) ++
      external.resources.flatMap(_.destination.validate(inputs)) ++
      external.resources.flatMap(resourceExists)
  }

  protected def listInputFiles(inputFiles: Vector[InputFile], context: Context)(implicit rng: RandomProvider, newFile: TmpDirectory, fileService: FileService): Vector[(Val[File], DeployedFile)] =
    inputFiles.map {
      case InputFile(prototype, name, link) ⇒ prototype → DeployedFile(context(prototype), name.from(context), link, deployedFileType = DeployedFileType.InputFile)
    }

  protected def listInputFileArray(inputFileArrays: Vector[InputFileArray], context: Context)(implicit rng: RandomProvider, newFile: TmpDirectory, fileService: FileService): Vector[(Val[Array[File]], Seq[DeployedFile])] =
    for {
      ifa ← inputFileArrays
    } yield {
      (
        ifa.prototype,
        context(ifa.prototype).zipWithIndex.map {
          case (file, i) ⇒
            DeployedFile(file, s"${ifa.prefix.from(context)}$i${ifa.suffix.from(context)}", link = ifa.link, deployedFileType = DeployedFileType.InputFile)
        }.toSeq
      )
    }

  protected def listResources(resources: Vector[External.Resource], context: Context, resolver: PathResolver)(implicit rng: RandomProvider, newFile: TmpDirectory, fileService: FileService): Iterable[DeployedFile] = {
    val byLocation =
      resources groupBy {
        case Resource(_, name, _, _) ⇒ resolver(name.from(context)).getCanonicalPath
      }

    val selectedOS =
      byLocation.toList flatMap {
        case (_, values) ⇒ values.find { _.os.compatible }
      }

    selectedOS.map {
      case Resource(file, name, link, _) ⇒ DeployedFile(file, name.from(context), link, deployedFileType = DeployedFileType.Resource)
    }
  }

  def relativeResolver(workDirectory: File)(filePath: String): File = {
    def resolved = workDirectory.resolve(filePath)
    resolved.toFile
  }

  private def copyFile(f: DeployedFile, to: File) = {
    to.createParentDirectory

    if (f.link) to.createLinkTo(f.file.getCanonicalFile)
    else {
      f.file.realFile.copy(to)
      to.applyRecursive { _.deleteOnExit }
    }
  }

  private def destination(resolver: PathResolver, f: DeployedFile) = resolver(f.expandedUserPath)

  def deployResources(external: External, context: Context, resolver: PathResolver)(implicit rng: RandomProvider, newFile: TmpDirectory, fileService: FileService) =
    for { f ← listResources(external.resources, context, resolver) } yield {
      val d = destination(resolver, f)
      copyFile(f, d)
      (f → d)
    }

  def deployInputFiles(external: External, context: Context, resolver: PathResolver)(implicit rng: RandomProvider, newFile: TmpDirectory, fileService: FileService) = {
    val (copiedFilesVariable, copiedFilesInfo) =
      listInputFiles(external.inputFiles, context).map {
        case (p, f) ⇒
          val d = destination(resolver, f)
          copyFile(f, d)
          (Variable(p, d), f → d)
      }.unzip

    val (copiedArrayFilesVariable, copiedFilesArrayInfo) =
      listInputFileArray(external.inputFileArrays, context).map {
        case (p, fs) ⇒
          val copied =
            fs.map { f ⇒
              val d = destination(resolver, f)
              copyFile(f, d)
              f → d
            }
          (Variable(p, copied.unzip._2.toArray), copied)
      }.unzip

    (context ++ copiedFilesVariable ++ copiedArrayFilesVariable, copiedFilesInfo ++ copiedFilesArrayInfo.flatten)
  }

  def deployInputFilesAndResources(external: External, context: Context, resolver: PathResolver)(implicit rng: RandomProvider, newFile: TmpDirectory, fileService: FileService) =
    deployAndListInputFiles(external: External, context, resolver)._1

  def deployAndListInputFiles(external: External, context: Context, resolver: PathResolver)(implicit rng: RandomProvider, newFile: TmpDirectory, fileService: FileService) = {
    val resourcesFiles = deployResources(external, context, resolver)
    val (newContext, inputFilesInfo) = deployInputFiles(external, context, resolver)
    (newContext, resourcesFiles ++ inputFilesInfo)
  }

  protected def outputFileVariables(outputFiles: Vector[External.OutputFile], context: Context, resolver: PathResolver)(implicit rng: RandomProvider, newFile: TmpDirectory, fileService: FileService) =
    outputFiles.map {
      case OutputFile(name, prototype) ⇒
        val fileName = name.from(context)
        val file = resolver(fileName)
        Variable(prototype, file)
    }

  def contextFiles(outputs: PrototypeSet, context: Context): Seq[Variable[File]] =
    InputOutputCheck.filterOutput(outputs, context).values.filter { v ⇒ v.prototype.`type` == ValType[File] }.map(_.asInstanceOf[Variable[File]]).toSeq

  def fetchOutputFiles(external: External, outputs: PrototypeSet, context: Context, resolver: PathResolver, workDirectories: Seq[File])(implicit rng: RandomProvider, newFile: TmpDirectory, fileService: FileService): Context = {
    val resultContext = listOutputFiles(external.outputFiles, outputs, context, resolver, workDirectories)
    val resultDirectory = newFile.newDir("externalresult")
    val outputContext = context ++ resultContext
    val result = outputContext ++ moveFilesOutOfWorkDirectory(outputs, outputContext, workDirectories, resultDirectory)
    fileService.deleteWhenEmpty(resultDirectory)
    result
  }

  def listOutputFiles(outputFiles: Vector[External.OutputFile], outputs: PrototypeSet, context: Context, resolver: PathResolver, workDirectories: Seq[File])(implicit rng: RandomProvider, newFile: TmpDirectory, fileService: FileService): Vector[Variable[File]] = {
    val fileOutputs = outputFileVariables(outputFiles, context, resolver)
    val allFiles = (fileOutputs ++ contextFiles(outputs, context)).distinct

    for {
      f ← allFiles
      if !f.value.exists
    } throw new UserBadDataError("Output file " + f.value.getAbsolutePath + s" (stored in variable ${f.prototype}) doesn't exist, parent directory ${f.value.getParentFileSafe} contains [" + f.value.getParentFileSafe.listFilesSafe.map(_.getName).mkString(", ") + "]")

    // If the file path contains a symbolic link, the link will be deleted by the cleaning operation
    val fetchedOutputFiles =
      allFiles.map { v ⇒ if (workDirectories.exists(_.isAParentOf(v.value))) Variable.copy(v)(value = v.value.realFile) else v }

    fetchedOutputFiles
  }

  def moveFilesOutOfWorkDirectory(outputs: PrototypeSet, context: Context, workDirectories: Seq[File], resultDirectory: File)(implicit fileService: FileService) = {
    val newFile = TmpDirectory(resultDirectory)

    contextFiles(outputs, context).map { v ⇒
      val movedFile =
        if (workDirectories.exists(_.isAParentOf(v.value))) {
          val newDir = newFile.newDir("outputFile")
          newDir.mkdirs()
          val moved = newDir / v.value.getName
          v.value.move(moved)
          fileService.deleteWhenEmpty(newDir)
          fileService.deleteWhenGarbageCollected(moved)
          moved
        }
        else v.value
      Variable.copy(v)(value = movedFile)
    }
  }

}

import org.openmole.plugin.task.external.External._

@Lenses case class External(
  inputFileArrays: Vector[External.InputFileArray] = Vector.empty,
  inputFiles:      Vector[External.InputFile]      = Vector.empty,
  outputFiles:     Vector[External.OutputFile]     = Vector.empty,
  resources:       Vector[External.Resource]       = Vector.empty
)