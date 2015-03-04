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

package org.openmole.core.tools.io

import com.ice.tar.{ TarEntry, TarConstants, TarInputStream, TarOutputStream }

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Stack

import java.io.{ File, IOException }
import java.nio.file._
import FileUtil._

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

    // new model using NIO
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

            // has the entry been marked as a symlink in the archive?
            if (!e.getLinkName.isEmpty) Files.createSymbolicLink(dest, Paths.get(e.getLinkName))
            // file copy from an InputStream does not support COPY_ATTRIBUTES, nor NOFOLLOW_LINKS
            else Files.copy(tis, dest)
          }
          // must set permissions explicitly from archive
          dest.toFile.mode = e.getMode
      }
    }
  }

  implicit class FileTarArchiveDecorator(file: File) {

    def archiveDirWithRelativePathNoVariableContent(toArchive: File) =
      withClosable(new TarOutputStream(file.bufferedOutputStream())) {
        _.createDirArchiveWithRelativePathNoVariableContent(toArchive)
      }

    //FIXME method name is ambiguous rename
    def archiveCompressDirWithRelativePathNoVariableContent(dest: File) =
      withClosable(new TarOutputStream(file.gzippedBufferedOutputStream)) {
        _.createDirArchiveWithRelativePathNoVariableContent(dest)
      }

    def extractDirArchiveWithRelativePath(dest: File) =
      withClosable(new TarInputStream(file.bufferedInputStream)) {
        _.extractDirArchiveWithRelativePath(dest)
      }

    def extractUncompressDirArchiveWithRelativePath(dest: File) =
      withClosable(new TarInputStream(file.gzippedBufferedInputStream)) {
        _.extractDirArchiveWithRelativePath(dest)
      }

  }

  private def createDirArchiveWithRelativePathWithAdditionalCommand(tos: TarOutputStream, baseDir: Path, additionalCommand: TarEntry ⇒ Unit) = {

    if (!Files.isDirectory(baseDir)) throw new IOException(baseDir.toString + " is not a directory.")

    val toArchive = new Stack[(Path, String)]
    toArchive.push((baseDir, ""))

    while (!toArchive.isEmpty) {

      val (source, entryName) = toArchive.pop

      // tar structure distinguishes symlinks
      val e =
        if (Files.isDirectory(source) && !Files.isSymbolicLink(source)) {
          // walk the directory tree to add all its entries to stack
          for (f ← Files.newDirectoryStream(source)) {
            val newSource = source.resolve(f.getFileName)
            val newEntryName = entryName + '/' + f.getFileName
            toArchive.push((newSource, newEntryName))
          }
          // create the actual tar entry for the directory
          new TarEntry(entryName + '/')
        }
        // tar distinguishes symlinks
        else if (Files.isSymbolicLink(source)) {
          val e = new TarEntry(entryName, TarConstants.LF_SYMLINK)
          e.setLinkName(Files.readSymbolicLink(source).toString)
          e
        }
        // plain files
        else {
          val e = new TarEntry(entryName)
          e.setSize(Files.size(source))
          e
        }

      // complete current entry by fixing its modes and writing it to the archive
      e.setMode(source.toFile.mode) // FIXME ugly explicit conversion
      additionalCommand(e)
      tos.putNextEntry(e)
      if (Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) try Files.copy(source, tos)
      finally tos.closeEntry
    }
  }

}
