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

import org.openmole.core.tools.service.OS
import org.openmole.core.dsl._
import org.openmole.core.dsl.`extension`._
import org.openmole.core.workflow.task.InputOutputCheck

object External:
  val PWD = Val[String]("PWD")

  trait ToInputFile[T]:
    def apply(t: T): InputFile

  case class InputFile(
    prototype:   Val[File],
    destination: FromContext[String],
    link:        Boolean
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

  enum DeployedFileType:
    case InputFile, Resource

  case class DeployedFile(file: File, expandedUserPath: String, link: Boolean, deployedFileType: DeployedFileType)
  type PathResolver = String ⇒ File

  def validate(external: External): Validate =
    def resourceExists(resource: External.Resource) = Validate:
      if !resource.file.exists()
      then Seq(new UserBadDataError(s"""File resource "${resource.file} doesn't exist."""))
      else Seq.empty

    external.inputFiles.flatMap(_.destination.validate) ++
      external.outputFiles.flatMap(_.origin.validate) ++
      external.resources.flatMap(_.destination.validate) ++
      external.resources.flatMap(resourceExists)

  protected def listInputFiles(inputFiles: Vector[InputFile], context: Context)(implicit rng: RandomProvider, newFile: TmpDirectory, fileService: FileService): Vector[(Val[File], DeployedFile)] =
    inputFiles.map {
      case InputFile(prototype, name, link) ⇒
        prototype → DeployedFile(context(prototype), name.from(context), link, deployedFileType = DeployedFileType.InputFile)
    }
  protected def listResources(resources: Vector[External.Resource], context: Context, resolver: PathResolver)(implicit rng: RandomProvider, newFile: TmpDirectory, fileService: FileService): Iterable[DeployedFile] =
    val byLocation =
      resources.zipWithIndex.groupBy: (resource, _) ⇒
        resolver(resource.destination.from(context)).getCanonicalPath

    val selectedOS =
      byLocation.toList flatMap: (_, values) ⇒
        values.find { _._1.os.compatible }

    selectedOS.sortBy(_._2).map(_._1).map: resource =>
       DeployedFile(resource.file, resource.destination.from(context), resource.link, deployedFileType = DeployedFileType.Resource)

  def relativeResolver(workDirectory: File)(filePath: String): File =
    def resolved = workDirectory.resolve(filePath)
    resolved.toFile

  private def copyFile(f: DeployedFile, to: File) =
    to.createParentDirectory
    if f.link
    then to.createLinkTo(f.file.getCanonicalFile)
    else f.file.realFile.copy(to)

  private def destination(resolver: PathResolver, f: DeployedFile) = resolver(f.expandedUserPath)

  def deployResources(external: External, context: Context, resolver: PathResolver)(implicit rng: RandomProvider, newFile: TmpDirectory, fileService: FileService) =
    for f ← listResources(external.resources, context, resolver)
    yield
      val d = destination(resolver, f)
      copyFile(f, d)
      f → d

  def deployInputFiles(external: External, context: Context, resolver: PathResolver)(implicit rng: RandomProvider, newFile: TmpDirectory, fileService: FileService): (Context, Iterable[(External.DeployedFile, File)]) =
    val (copiedFilesVariable, copiedFilesInfo) =
      listInputFiles(external.inputFiles, context).map { case (p, f) ⇒
        val d = destination(resolver, f)
        copyFile(f, d)
        (Variable(p, d), f → d)
      }.unzip

    (context ++ copiedFilesVariable, copiedFilesInfo)

  def deployInputFilesAndResources(external: External, context: Context, resolver: PathResolver)(implicit rng: RandomProvider, newFile: TmpDirectory, fileService: FileService) =
    deployAndListInputFiles(external: External, context, resolver)._1

  def deployAndListInputFiles(external: External, context: Context, resolver: PathResolver)(implicit rng: RandomProvider, newFile: TmpDirectory, fileService: FileService) =
    val resourcesFiles = deployResources(external, context, resolver)
    val (newContext, inputFilesInfo) = deployInputFiles(external, context, resolver)
    (newContext, resourcesFiles ++ inputFilesInfo)

  protected def outputFileVariables(outputFiles: Vector[External.OutputFile], context: Context, resolver: PathResolver)(implicit rng: RandomProvider, newFile: TmpDirectory, fileService: FileService) =
    outputFiles.map:
      case OutputFile(name, prototype) ⇒
        val fileName = name.from(context)
        val file = resolver(fileName)
        Variable(prototype, file)

  def contextFiles(outputs: PrototypeSet, context: Context): Seq[Variable[File]] =
    InputOutputCheck.filterOutput(outputs, context).values.filter { v ⇒ v.prototype.`type` == ValType[File] }.map(_.asInstanceOf[Variable[File]]).toSeq

  def fetchOutputFiles(external: External, outputs: PrototypeSet, context: Context, resolver: PathResolver, workDirectories: Seq[File])(implicit rng: RandomProvider, newFile: TmpDirectory, fileService: FileService): Context =
    val resultContext = listOutputFiles(external.outputFiles, outputs, context, resolver, workDirectories)
    val resultDirectory = newFile.newDir("externalresult")
    val outputContext = context ++ resultContext
    val result = outputContext ++ moveFilesOutOfWorkDirectory(outputs, outputContext, workDirectories, resultDirectory)
    fileService.deleteWhenEmpty(resultDirectory)
    result

  def listOutputFiles(outputFiles: Vector[External.OutputFile], outputs: PrototypeSet, context: Context, resolver: PathResolver, workDirectories: Seq[File])(implicit rng: RandomProvider, newFile: TmpDirectory, fileService: FileService): Vector[Variable[File]] =
    val fileOutputs = outputFileVariables(outputFiles, context, resolver)
    val allFiles = (fileOutputs ++ contextFiles(outputs, context)).distinct

    for
      f ← allFiles
      if !f.value.exists
    do
      def parentMessage =
        if f.value.getParentFileSafe.exists()
        then s"""its parent directory ${f.value.getParentFileSafe} contains [${f.value.getParentFileSafe.listFilesSafe.map(_.getName).mkString(", ")}]"""
        else s"""its parent directory ${f.value.getParentFileSafe} doesn't exist either"""
      throw new UserBadDataError(s"""Output file ${f.value.getAbsolutePath} (stored in variable ${f.prototype}) doesn't exist, $parentMessage""")

    // If the file path contains a symbolic link, the link will be deleted by the cleaning operation
    val fetchedOutputFiles =
      allFiles.map { v ⇒ if (workDirectories.exists(_.isAParentOf(v.value))) Variable.copy(v)(value = v.value.realFile) else v }

    fetchedOutputFiles

  def moveFilesOutOfWorkDirectory(outputs: PrototypeSet, context: Context, workDirectories: Seq[File], resultDirectory: File)(implicit fileService: FileService) =
    val newFile = TmpDirectory(resultDirectory)

    contextFiles(outputs, context).map: v ⇒
      val movedFile =
        if workDirectories.exists(_.isAParentOf(v.value))
        then
          val newDir = newFile.newDir("outputFile")
          newDir.mkdirs()
          val moved = fileService.wrapRemoveOnGC(newDir / v.value.getName)
          v.value.move(moved)
          fileService.deleteWhenEmpty(newDir)
          moved
        else v.value
      Variable.copy(v)(value = movedFile)



import org.openmole.plugin.task.external.External._

case class External(
  inputFiles:      Vector[External.InputFile]      = Vector.empty,
  outputFiles:     Vector[External.OutputFile]     = Vector.empty,
  resources:       Vector[External.Resource]       = Vector.empty
)