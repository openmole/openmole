package org.openmole.core.workspace

import org.openmole.core.workspace.Workspace._
import org.openmole.tool.file._

object NewFile {
  def apply(workspace: Workspace): NewFile = NewFile(workspace.tmpDir)
}

case class NewFile(baseDir: File) {
  def makeNewDir(prefix: String = fixedDir): File = {
    val dir = newDir(prefix)
    dir.mkdirs()
    dir
  }

  def newDir(prefix: String = fixedDir): File = baseDir.newDir(prefix)
  def newFile(prefix: String = fixedPrefix, suffix: String = fixedPostfix): File = baseDir.newFile(prefix, suffix)
  def withTmpFile[T](prefix: String, postfix: String)(f: File ⇒ T): T = {
    val file = newFile(prefix, postfix)
    try f(file)
    finally file.delete
  }

  def withTmpFile[T](f: File ⇒ T): T = {
    val file = newFile()
    try f(file)
    finally file.delete
  }

  def withTmpDir[T](f: File ⇒ T): T = {
    val file = newFile()
    try {
      file.mkdir()
      f(file)
    }
    finally file.recursiveDelete
  }
}

