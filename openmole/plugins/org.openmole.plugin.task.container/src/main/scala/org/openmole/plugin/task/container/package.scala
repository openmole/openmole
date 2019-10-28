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
import org.openmole.core.expansion.FromContext
import org.openmole.core.dsl._
import org.openmole.plugin.task.external._
import org.openmole.core.workflow.validation._

package container {

  import monocle.Lens

  object HostFile {
    implicit def tupleToHostFile(t: (String, String)) = HostFile(t._1, t._2)
  }

  case class HostFile(path: String, destination: String)

  trait HostFiles[T] {
    def hostFiles: Lens[T, Vector[HostFile]]
  }

  trait ContainerPackage {
    lazy val hostFiles = new {
      def +=[T: HostFiles](path: String, destination: OptionalArgument[String] = None): T ⇒ T =
        implicitly[HostFiles[T]].hostFiles add HostFile(path, destination.getOrElse(path))
    }
  }

  object ContainerImage {
    implicit def fileToContainerImage(f: java.io.File) = {
      def compressed = f.getName.endsWith(".tgz") || f.getName.endsWith(".gz")
      SavedDockerImage(f, compressed)
    }
    implicit def stringToContainerImage(s: String) =
      if (s.contains(":")) {
        val Vector(image, tag) = s.split(":").toVector
        DockerImage(image, tag)
      }
      else DockerImage(s)

  }

  sealed trait ContainerImage
  case class DockerImage(image: String, tag: String = "latest", registry: String = "https://registry-1.docker.io") extends ContainerImage
  case class SavedDockerImage(file: java.io.File, compressed: Boolean) extends ContainerImage

}

package object container extends ContainerPackage {

  type FileBinding = (String, String)

  /**
   * FIXME maybe make it an option to avoid passing "" when inputDirectory is empty
   *
   * @param inputDirectory Directory used to store input files / folder from the dataflow
   * @param baseDirectory
   * @param path Target location
   * @return
   */
  def inputPathResolver(inputDirectory: File, baseDirectory: String)(path: String): File = {
    if (File(path).isAbsolute) inputDirectory / path
    else inputDirectory / baseDirectory / path
  }

  def outputPathResolver(preparedFileBindings: Iterable[FileBinding], hostFileBindings: Iterable[FileBinding], inputDirectory: File, userWorkDirectory: String, rootDirectory: File)(filePath: String): File = {

    /**
     * Search for a parent, not only in level 1 subdirs
     * @param dir potential parent
     * @param file target file
     * @return true if dir is a parent of file at a level
     */
    def isOneOfParents(dir: String, file: String) = File(file).getAbsolutePath.startsWith(File(dir).getAbsolutePath)
    def isPreparedFile(f: String) = preparedFileBindings.map(b ⇒ b._2).exists(b ⇒ isOneOfParents(b, f))
    def isHostFile(f: String) = hostFileBindings.map(b ⇒ b._2).exists(b ⇒ isOneOfParents(b, f))
    def isAbsolute = File(filePath).isAbsolute

    val absolutePathInArchive: String = if (isAbsolute) filePath else (File(userWorkDirectory) / filePath).getPath
    val pathToResolve = (File("/") / absolutePathInArchive).getAbsolutePath

    if (isPreparedFile(pathToResolve)) inputPathResolver(inputDirectory, userWorkDirectory)(filePath)
    else if (isHostFile(pathToResolve)) File("/") / absolutePathInArchive
    else rootDirectory / absolutePathInArchive
  }

  def validateContainer(
    commands:             Vector[FromContext[String]],
    environmentVariables: Vector[EnvironmentVariable],
    external:             External,
    inputs:               PrototypeSet
  ) = Validate { p ⇒
    import p._

    val allInputs = External.PWD :: inputs.toList
    val validateVariables = environmentVariables.flatMap(v ⇒ Seq(v.name, v.value)).flatMap(_.validate(allInputs))

    commands.flatMap(_.validate(allInputs)) ++
      validateVariables ++
      External.validate(external)(allInputs).apply
  }

  def ArchiveNotFound(archive: File) = Seq(new UserBadDataError(s"Cannot find specified Archive $archive in your work directory. Did you prefix the path with `workDirectory / `?"))

  lazy val ArchiveOK = Seq.empty[UserBadDataError]

  sealed trait ContainerSystem
  case class Proot(noSeccomp: Boolean = false, kernel: String = "3.2.1") extends ContainerSystem

}
