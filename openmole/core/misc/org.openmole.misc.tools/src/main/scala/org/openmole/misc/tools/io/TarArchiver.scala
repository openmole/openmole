/*
 * Copyright (C) 2010 Romain Reuillon
 * Copyright (C) 2014 Jonathan Passerat-Palmbach
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.misc.tools.io

import com.ice.tar.{ TarEntry, TarConstants, TarInputStream, TarOutputStream }

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Stack

import java.io.{ File, IOException }
import java.nio.file._
import org.openmole.misc.tools.io.FileUtil._

// provide scala foreach over Java collections
import scala.collection.JavaConversions._
import java.util

object TarArchiver {

  implicit class TarOutputStreamDecorator(tos: TarOutputStream) {
    def addFile(f: Path, name: String) = {
      val entry = new TarEntry(name)
      entry.setSize(Files.size(f))
      entry.setMode(f.toFile.mode) // FIXME ugly explicit conversion
      tos.putNextEntry(entry)
      try Files.copy(f, tos) finally tos.closeEntry
    }

    def createDirArchiveWithRelativePathNoVariableContent(baseDir: Path) = createDirArchiveWithRelativePathWithAdditionalCommand(tos, baseDir, (e: TarEntry) ⇒ e.setModTime(0))
    def createDirArchiveWithRelativePath(baseDir: Path) = createDirArchiveWithRelativePathWithAdditionalCommand(tos, baseDir, { (e) ⇒ })
  }

  implicit class TarInputStreamDecorator(tis: TarInputStream) {

    def applyAndClose[T](f: TarEntry ⇒ T): Iterable[T] = try {
      val ret = new ListBuffer[T]

      var e = tis.getNextEntry
      while (e != null) {
        ret += f(e)
        e = tis.getNextEntry
      }
      ret
    }
    finally tis.close

    // new implementation using NIO
    def extractDirArchiveWithRelativePath(baseDir: Path) = {
      if (!Files.isDirectory(baseDir)) throw new IOException(baseDir.toString + " is not a directory.")

      Iterator.continually(tis.getNextEntry).takeWhile(_ != null).foreach {
        e ⇒
          val dest = Paths.get(baseDir.toString, e.getName)
          if (e.isDirectory) {
            Files.createDirectories(dest)
          }
          else {
            Files.createDirectories(dest.getParent)
            // copy from an InputStream does not support COPY_ATTRIBUTES, nor NOFOLLOW_LINKS
            Files.copy(tis, dest)
            // must set permissions explicitly from archive
            dest.toFile.mode = e.getMode
          }
      }
    }
  }

  implicit class FileTarArchiveDecorator(file: File) {

    def archiveDirWithRelativePathNoVariableContent(toArchive: File) = {
      val os = new TarOutputStream(file.gzippedBufferedOutputStream)
      try os.createDirArchiveWithRelativePathNoVariableContent(toArchive)
      finally os.close
    }

    //FIXME method name is ambiguous rename
    def archiveCompressDirWithRelativePathNoVariableContent(dest: File) = {
      val os = new TarOutputStream(file.gzippedBufferedOutputStream)
      try os.createDirArchiveWithRelativePathNoVariableContent(dest)
      finally os.close
    }

    def extractDirArchiveWithRelativePath(dest: File) = {
      val is = new TarInputStream(file.bufferedInputStream)
      try is.extractDirArchiveWithRelativePath(dest)
      finally is.close
    }

    def extractUncompressDirArchiveWithRelativePath(dest: File) = {
      val is = new TarInputStream(file.gzippedBufferedInputStream)
      try is.extractDirArchiveWithRelativePath(dest)
      finally is.close
    }
  }

  // new version using NIO
  private def createDirArchiveWithRelativePathWithAdditionalCommand(tos: TarOutputStream, baseDir: Path, additionalCommand: TarEntry ⇒ Unit) = {

    if (!Files.isDirectory(baseDir)) throw new IOException(baseDir.toString + " is not a directory.")

    val toArchive = new Stack[(Path, String)]
    toArchive.push((baseDir, ""))

    while (!toArchive.isEmpty) {

      val (source, entryName) = toArchive.pop

      val e =
        if (Files.isDirectory(source)) {
          // walk the directory tree to add all its entries to stack
          for (f ← Files.newDirectoryStream(source)) {
            val newSource = source.resolve(f.getFileName)
            val newEntryName = entryName + '/' + f.getFileName
            toArchive.push((newSource, newEntryName))
          }
          // create the actual tar entry for the directory
          new TarEntry(entryName + '/')
        }
        // tar distinguishes symlinks, but the com.ice.tar's implementation decided not to do so...
        // so any kind of files goes in there
        else {
          val e = new TarEntry(entryName)
          e.setSize(Files.size(source))
          e
        }

      // complete current entry by fixing its modes and writing it to the archive
      e.setMode(source.toFile.mode) // FIXME ugly explicit conversion
      additionalCommand(e)
      tos.putNextEntry(e)
      if (!Files.isDirectory(source)) try Files.copy(source, tos) finally tos.closeEntry
    }
  }

}
