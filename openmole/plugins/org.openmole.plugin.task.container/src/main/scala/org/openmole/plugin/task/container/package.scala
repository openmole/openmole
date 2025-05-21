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

package org.openmole.plugin.task.container

import org.openmole.core.context.PrototypeSet
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.argument.FromContext
import org.openmole.core.dsl._
import org.openmole.core.dsl.`extension`._
import org.openmole.plugin.task.external._
import org.openmole.core.workflow.validation.*
import org.openmole.tool.cache.*

import monocle.Focus

object HostFile:
  implicit def tupleToHostFile(t: (String, String)): HostFile = HostFile(t._1, t._2)

case class HostFile(path: String, destination: String)

object ContainerImage:
  given Conversion[String, ContainerImage] = fromString
  given Conversion[File, ContainerImage] = fromFile

  def fromFile(f: java.io.File): ContainerImage =
    def compressed = f.getName.endsWith(".tgz") || f.getName.endsWith(".gz")
    SavedDockerImage(f, compressed)

  def fromString(s: String): ContainerImage =
    if s.contains(":")
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

type FileBinding = (String, String)

def outputPathResolver(fileBindings: Seq[FileBinding], rootDirectory: File, containerPathResolver: String => File )(filePath: String): File =
  /**
   * Search for a parent, not only in level 1 subdirs
   * @param dir potential parent
   * @param file target file
   * @return true if dir is a parent of file at a level
   */
  def isOneOfParents(dir: File, file: File) = file.getAbsolutePath.startsWith(dir.getAbsolutePath)

  def relativiseFromParent(parent: File, file: File): File =
    if isOneOfParents(parent, file)
    then file.getAbsolutePath.drop(parent.getAbsolutePath.length)
    else file

  def resolveFile(f: File) =
    fileBindings.
      map((local, bind) => (local, containerPathResolver(bind))).
      sortBy((_, bind) => bind.getPath.split("/").length).
      findLast((_, bind) => isOneOfParents(bind, f)).
      map: (local, bind) =>
        File(local) / relativiseFromParent(bind, f).getPath

  def absolutePathInArchive = containerPathResolver(filePath)
  def pathToResolve = containerPathResolver(filePath)

  resolveFile(absolutePathInArchive) getOrElse (rootDirectory / absolutePathInArchive.getPath)

def ArchiveNotFound(archive: File) = Seq(new UserBadDataError(s"Cannot find specified Archive $archive in your work directory. Did you prefix the path with `workDirectory / `?"))

lazy val ArchiveOK = Seq.empty[UserBadDataError]

object InstalledSingularityImage:
  def sudo(containerSystem: ContainerSystem, cmd: String) = s"fakeroot $cmd"

  type OverlayKey = CacheKey[WithInstance[_root_.container.Singularity.OverlayImage]]
  type FlatImageKey = CacheKey[WithInstance[FlatContainerTask.Cached]]

  extension (img: InstalledSingularityImage)
    def workDirectory =
      img match
        case i: InstalledSIFOverlayImage => i.image.workDirectory
        case i: InstalledSIFMemoryImage => i.image.workDirectory
        case i: InstalledFlatImage => i.image.workDirectory

  case class InstalledSIFMemoryImage(image: _root_.container.Singularity.SingularityImageFile, containerSystem: SingularityMemory) extends InstalledSingularityImage:
    lazy val cacheKey: OverlayKey = CacheKey()

  case class InstalledSIFOverlayImage(image: _root_.container.Singularity.SingularityImageFile, containerSystem: SingularityOverlay, overlay: Option[_root_.container.Singularity.OverlayImage] = None) extends InstalledSingularityImage:
    lazy val cacheKey: OverlayKey = CacheKey()

  case class InstalledFlatImage(image: _root_.container.FlatImage, containerSystem: SingularityFlatImage) extends InstalledSingularityImage:
    lazy val cacheKey: FlatImageKey = CacheKey()


  type InstalledSIFImage = InstalledSIFMemoryImage | InstalledSIFOverlayImage

sealed trait InstalledSingularityImage


/**
 * Trait for either string scripts or script file runnable in tasks based on the container task
 */
object RunnableScript:
  implicit def stringToRunnableScript(s: String): RunnableScript = RawScript(s)
  implicit def fileToRunnableScript(f: File): RunnableScript = FileScript(f)

  def content(script: RunnableScript): String =
    script match
      case RawScript(s)  => s
      case FileScript(f) => f.content


sealed trait RunnableScript
case class RawScript(rawscript: String) extends RunnableScript
case class FileScript(file: File) extends RunnableScript

object JavaConfiguration:
  def fewerThreadsParameters: Seq[String] = Seq("-XX:+UseG1GC", "-XX:ParallelGCThreads=1", "-XX:CICompilerCount=2", "-XX:ConcGCThreads=1", "-XX:G1ConcRefinementThreads=1")
  