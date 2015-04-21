/*
 * Copyright (C) 2015 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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
package org.openmole.tool

import java.io.{ IOException, File }
import java.nio.file._
import org.openmole.tool.file._
import org.openmole.tool.tar.{ TarConstants, TarInputStream, TarEntry, TarOutputStream }

import scala.collection.mutable.{ Stack, ListBuffer }
import collection.JavaConversions._

package object tar {

  implicit class TarOutputStreamDecorator(tos: TarOutputStream) {
    def addFile(f: File, name: String) = {
      val entry = new TarEntry(name)
      entry.setSize(Files.size(f))
      entry.setMode(f.mode)
      tos.putNextEntry(entry)
      try Files.copy(f, tos) finally tos.closeEntry
    }

    def archive(directory: File, time: Boolean = true) =
      createDirArchiveWithRelativePathWithAdditionalCommand(tos, directory, if (time) identity(_) else _.setModTime(0))
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
    def extract(directory: File) = {
      if (!Files.isDirectory(directory)) throw new IOException(directory.toString + " is not a directory.")

      Iterator.continually(tis.getNextEntry).takeWhile(_ != null).foreach {
        e ⇒
          val dest = Paths.get(directory.toString, e.getName)
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

    def archive(archive: File, time: Boolean = true) =
      withClosable(new TarOutputStream(file.bufferedOutputStream())) {
        _.archive(archive, time)
      }

    //FIXME method name is ambiguous rename
    def archiveCompress(dest: File, time: Boolean = true) =
      withClosable(new TarOutputStream(file.gzippedBufferedOutputStream)) {
        _.archive(dest, time)
      }

    def extract(dest: File) =
      withClosable(new TarInputStream(file.bufferedInputStream)) {
        _.extract(dest)
      }

    def extractUncompress(dest: File) =
      withClosable(new TarInputStream(file.gzippedBufferedInputStream)) {
        _.extract(dest)
      }

    def copyCompress(toF: File): File = {
      if (toF.isDirectory) toF.archiveCompress(file)
      else file.copyCompressFile(toF)
      toF
    }

    def tarOutputStream = new TarOutputStream(file.bufferedOutputStream())

    def withTarOutputStream[T] = withClosable[TarOutputStream, T](new TarOutputStream(file.bufferedOutputStream()))(_)
    def withTarGZOutputStream[T] = withClosable[TarOutputStream, T](new TarOutputStream(file.bufferedOutputStream().toGZ))(_)
  }

  private def createDirArchiveWithRelativePathWithAdditionalCommand(tos: TarOutputStream, directory: File, additionalCommand: TarEntry ⇒ Unit) = {

    if (!Files.isDirectory(directory)) throw new IOException(directory.toString + " is not a directory.")

    val toArchive = new Stack[(File, String)]
    toArchive.push(directory -> "")

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
      if (source != directory) {
        e.setMode(source.mode)
        additionalCommand(e)
        tos.putNextEntry(e)
        if (Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) try Files.copy(source, tos)
        finally tos.closeEntry
      }
    }
  }
}
