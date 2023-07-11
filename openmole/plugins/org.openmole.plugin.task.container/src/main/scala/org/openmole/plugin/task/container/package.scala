/*
 * Copyright (C) 2017 Jonathan Passerat-Palmbach
 * Copyright (C) 2017 Romain Reuillon
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

package org.openmole.plugin.task

import org.openmole.core.context.PrototypeSet
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.fromcontext.FromContext
import org.openmole.core.dsl._
import org.openmole.core.dsl.`extension`._
import org.openmole.plugin.task.external._
import org.openmole.core.workflow.validation._

package container:

  import monocle.Focus

  object HostFile:
    implicit def tupleToHostFile(t: (String, String)): HostFile = HostFile(t._1, t._2)

  case class HostFile(path: String, destination: String)

  object ContainerImage:
    implicit def fileToContainerImage(f: java.io.File): ContainerImage =
      def compressed = f.getName.endsWith(".tgz") || f.getName.endsWith(".gz")
      SavedDockerImage(f, compressed)

    implicit def stringToContainerImage(s: String): ContainerImage =
      if (s.contains(":"))
      then
        val Vector(image, tag) = s.split(":").toVector
        DockerImage(image, tag)
      else DockerImage(s)

  object DockerImage:
    def toRegistryImage(image: DockerImage) =
      _root_.container.RegistryImage(
        name = image.image,
        tag = image.tag,
        registry = image.registry
      )

  sealed trait ContainerImage
  case class DockerImage(image: String, tag: String = "latest", registry: String = "https://registry-1.docker.io") extends ContainerImage
  case class SavedDockerImage(file: java.io.File, compressed: Boolean) extends ContainerImage



package object container:

  type FileBinding = (String, String)


  def outputPathResolver(fileBindings: Seq[FileBinding], userWorkDirectory: String, rootDirectory: File)(filePath: String): File =

    /**
     * Search for a parent, not only in level 1 subdirs
     * @param dir potential parent
     * @param file target file
     * @return true if dir is a parent of file at a level
     */
    def isOneOfParents(dir: String, file: String) = File(file).getAbsolutePath.startsWith(File(dir).getAbsolutePath)

    def relativiseFromParent(parent: String, file: String): String =
      if isOneOfParents(parent, file)
      then File(file).getAbsolutePath.drop(File(parent).getAbsolutePath.length)
      else file

    def resolveFile(f: String) =
      fileBindings.
        findLast((_, bindPath) ⇒ isOneOfParents(bindPath, f)).
        map((localPath, bindPath) => File(localPath) / relativiseFromParent(bindPath, f))

    def isAbsolute = File(filePath).isAbsolute
    def absolutePathInArchive: String = if isAbsolute then filePath else (File(userWorkDirectory) / filePath).getPath

    def pathToResolve = (File("/") / absolutePathInArchive).getAbsolutePath

    resolveFile(pathToResolve) getOrElse (rootDirectory / absolutePathInArchive)

  def validateContainer(
    commands:             Seq[FromContext[String]],
    environmentVariables: Seq[EnvironmentVariable],
    external:             External
  ): Validate = Validate: p ⇒
    import p._

    val allInputs = External.PWD :: p.inputs.toList
    val validateVariables = environmentVariables.flatMap(v ⇒ Seq(v.name, v.value)).flatMap(_.validate(allInputs))

    commands.flatMap(_.validate(allInputs)) ++
      validateVariables ++
      External.validate(external)(allInputs)


  def ArchiveNotFound(archive: File) = Seq(new UserBadDataError(s"Cannot find specified Archive $archive in your work directory. Did you prefix the path with `workDirectory / `?"))

  lazy val ArchiveOK = Seq.empty[UserBadDataError]

  object ContainerSystem:
    def default = Singularity()

    def sudo(containerSystem: ContainerSystem, cmd: String) =
      containerSystem match
        case _: Proot       ⇒ s"sudo $cmd"
        case _: Singularity ⇒ s"fakeroot $cmd"

  sealed trait ContainerSystem
  case class Proot(proot: File, noSeccomp: Boolean = false, kernel: String = "3.2.1") extends ContainerSystem
  case class Singularity(command: String = "singularity") extends ContainerSystem

  type InstalledImage = _root_.container.FlatImage

  /**
   * Trait for either string scripts or script file runnable in tasks based on the container task
   */
  object RunnableScript:
    implicit def stringToRunnableScript(s: String): RunnableScript = RawScript(s)
    implicit def fileToRunnableScript(f: File): RunnableScript = FileScript(f)

    def content(script: RunnableScript): String =
      script match
        case RawScript(s)  ⇒ s
        case FileScript(f) ⇒ f.content


  sealed trait RunnableScript
  case class RawScript(rawscript: String) extends RunnableScript
  case class FileScript(file: File) extends RunnableScript

