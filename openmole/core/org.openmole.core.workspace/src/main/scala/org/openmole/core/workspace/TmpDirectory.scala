package org.openmole.core.workspace

import java.util.UUID

import org.openmole.core.workspace.Workspace._
import org.openmole.tool.file._

object TmpDirectory:

  def apply(workspace: Workspace): TmpDirectory = TmpDirectory(workspace.tmpDirectory)
  def dispose(newFile: TmpDirectory) = newFile.directory.recursiveDelete

  def stub() =
    val tmpDirectory = java.io.File.createTempFile("tmp", "directory")
    tmpDirectory.delete()
    TmpDirectory(tmpDirectory)

  def makeNewDir(prefix: String = fixedDir)(using TmpDirectory): File =
    val dir = newDirectory(prefix)
    dir.mkdirs()
    dir

  def newDirectory(prefix: String = fixedDir, create: Boolean = false)(using tmpDirectory: TmpDirectory): File = tmpDirectory.directory.newDirectory(prefix, create = create)
  def newFile(prefix: String = fixedPrefix, suffix: String = fixedPostfix)(using tmpDirectory: TmpDirectory): File = tmpDirectory.directory.newFile(prefix, suffix)

  def withTmpFile[T](prefix: String, postfix: String)(f: File => T)(using TmpDirectory): T =
    val file = newFile(prefix, postfix)
    try f(file)
    finally file.delete

  def withTmpFile[T](f: File => T)(using TmpDirectory): T =
    val file = newFile()
    try f(file)
    finally file.delete

  def withTmpDir[T](f: File => T)(using TmpDirectory): T =
    val file = newFile()
    try
      file.mkdirs()
      f(file)
    finally file.recursiveDelete

case class TmpDirectory(directory: File):
  export TmpDirectory.{makeNewDir, newDirectory, newFile, withTmpFile, withTmpDir}