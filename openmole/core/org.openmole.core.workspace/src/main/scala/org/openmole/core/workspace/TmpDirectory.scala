package org.openmole.core.workspace

import java.util.UUID

import org.openmole.core.workspace.Workspace._
import org.openmole.tool.file._

object TmpDirectory {
  def apply(workspace: Workspace): TmpDirectory = TmpDirectory(workspace.tmpDirectory)
  def dispose(newFile: TmpDirectory) = newFile.directory.recursiveDelete
}

case class TmpDirectory(directory: File):
  def makeNewDir(prefix: String = fixedDir): File =
    val dir = newDir(prefix)
    dir.mkdirs()
    dir

  def newDir(prefix: String = fixedDir, create: Boolean = false): File = directory.newDirectory(prefix, create = create)
  def newFile(prefix: String = fixedPrefix, suffix: String = fixedPostfix): File = directory.newFile(prefix, suffix)
  def withTmpFile[T](prefix: String, postfix: String)(f: File ⇒ T): T =
    val file = newFile(prefix, postfix)
    try f(file)
    finally file.delete

  def withTmpFile[T](f: File ⇒ T): T =
    val file = newFile()
    try f(file)
    finally file.delete

  def withTmpDir[T](f: File ⇒ T): T =
    val file = newFile()
    try
      file.mkdirs()
      f(file)
    finally file.recursiveDelete


