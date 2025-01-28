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
  implicit def fileToContainerImage(f: java.io.File): ContainerImage =
    def compressed = f.getName.endsWith(".tgz") || f.getName.endsWith(".gz")
    SavedDockerImage(f, compressed)

  implicit def stringToContainerImage(s: String): ContainerImage =
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
      findLast((_, bind) ⇒ isOneOfParents(bind, f)).
      map: (local, bind) =>
        File(local) / relativiseFromParent(bind, f).getPath

  def absolutePathInArchive = containerPathResolver(filePath)
  def pathToResolve = containerPathResolver(filePath)

  resolveFile(absolutePathInArchive) getOrElse (rootDirectory / absolutePathInArchive.getPath)

def ArchiveNotFound(archive: File) = Seq(new UserBadDataError(s"Cannot find specified Archive $archive in your work directory. Did you prefix the path with `workDirectory / `?"))

lazy val ArchiveOK = Seq.empty[UserBadDataError]

object ContainerSystem:
  def default: ContainerSystem = SingularityOverlay()
  def sudo(containerSystem: ContainerSystem, cmd: String) = s"fakeroot $cmd"

  type OverlayKey = CacheKey[WithInstance[_root_.container.Singularity.OverlayImage]]
  type FlatImageKey = CacheKey[WithInstance[FlatContainerTask.Cached]]

  object InstalledImage:
    extension (img: InstalledImage)
      def image =
        img match
          case i: InstalledSIFImage => i.image
          case i: InstalledFlatImage => i.image

  sealed trait InstalledImage

  case class InstalledSIFImage(image: _root_.container.Singularity.SingularityImageFile, containerSystem: SingularitySIF) extends InstalledImage:
    lazy val cacheKey: ContainerSystem.OverlayKey = CacheKey()

  case class InstalledFlatImage(image: _root_.container.FlatImage, containerSystem: SingularityFlatImage) extends InstalledImage:
    lazy val cacheKey: ContainerSystem.FlatImageKey = CacheKey()

  type SingularitySIF = SingularityOverlay | SingularityMemory


sealed trait ContainerSystem

case class SingularityOverlay(reuse: Boolean = true, size: Information = 20.gigabyte, verbose: Boolean = false, copy: Boolean = false, overlay: Option[_root_.container.Singularity.OverlayImage] = None) extends ContainerSystem
case class SingularityMemory(verbose: Boolean = false) extends ContainerSystem
case class SingularityFlatImage(duplicateImage: Boolean = true, reuseContainer: Boolean = true, verbose: Boolean = false, isolatedDirectories:  Seq[String] = Seq()) extends ContainerSystem

export ContainerSystem.{InstalledImage as InstalledContainerImage}

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

