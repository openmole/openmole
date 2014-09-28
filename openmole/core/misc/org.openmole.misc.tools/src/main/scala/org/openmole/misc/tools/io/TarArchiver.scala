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

import com.ice.tar.TarEntry
import com.ice.tar.TarConstants
import com.ice.tar.TarInputStream
import com.ice.tar.TarOutputStream

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Stack

import java.io.IOException
import java.nio.file.{ LinkOption, StandardCopyOption, Path, FileSystems, Files }
import java.nio.file.attribute.PosixFilePermission

import scala.collection.JavaConversions._ // provide scala foreach over Java collections
import scala.collection.JavaConverters._ // convert Java Set to Scala

object TarArchiver {

  implicit def TarInputStream2TarInputStreamDecorator(tis: TarInputStream) = new TarInputStreamDecorator(tis)
  implicit def TarOutputStream2TarOutputStreamComplement(tos: TarOutputStream) = new TarOutputStreamDecorator(tos)
  implicit def javaSet2ScalaSet(javaSet: java.util.Set[PosixFilePermission]) = (javaSet asScala) toSet

  class TarOutputStreamDecorator(tos: TarOutputStream) {

    def addFile(f: Path, name: String) = {
      val entry = new TarEntry(name)
      entry.setSize(Files.size(f))
      entry.setMode(FileUtil.permissionsToTarMode(Files.getPosixFilePermissions(f)))
      tos.putNextEntry(entry)
      try Files.copy(f, tos) finally tos.closeEntry
    }

    def createDirArchiveWithRelativePathNoVariableContent(baseDir: Path) = createDirArchiveWithRelativePathWithAdditionalCommand(tos, baseDir, (e: TarEntry) ⇒ e.setModTime(0))
    def createDirArchiveWithRelativePath(baseDir: Path) = createDirArchiveWithRelativePathWithAdditionalCommand(tos, baseDir, { (e) ⇒ })
  }

  class TarInputStreamDecorator(tis: TarInputStream) {

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

    // TODO do we really need to distinguish all the cases?
    def extractDirArchiveWithRelativePath(baseDir: Path) = {
      if (!Files.isDirectory(baseDir)) throw new IOException(baseDir.toString + " is not a directory.")

      val fs = FileSystems.getDefault
      val links = Iterator.continually(tis.getNextEntry).takeWhile(_ != null).flatMap {
        e ⇒
          val dest = fs.getPath(baseDir.toString, e.getName)
          val symlink =
            if (!e.getLinkName.isEmpty) Some(dest -> e.getLinkName)
            else if (e.isDirectory) {
              Files.createDirectories(dest)
              None
            }
            else {
              Files.createDirectories(dest.getParent)
              Files.copy(tis, dest, StandardCopyOption.COPY_ATTRIBUTES, LinkOption.NOFOLLOW_LINKS)
              None
            }
          symlink
      }.toList

      // FIXME useless now?
      //
      //      links.foreach {
      //        case ((dest, name)) ⇒ Files.createSymbolicLink()
      //      }

    }
  }

  // TODO do we really need to distinguish all the cases?
  private def createDirArchiveWithRelativePathWithAdditionalCommand(tos: TarOutputStream, baseDir: Path, additionalCommand: TarEntry ⇒ Unit) = {

    if (!Files.isDirectory(baseDir)) throw new IOException(baseDir.toString + " is not a directory.")

    val toArchive = new Stack[(Path, String)]
    toArchive.push((baseDir, ""))

    var links = List.empty[(Path, String)]
    val fs = FileSystems.getDefault

    while (!toArchive.isEmpty) {
      val (source, entryName) = toArchive.pop
      // tar structure distinguishes symlinks
      if (Files.isSymbolicLink(source)) links ::= source -> entryName
      else {
        val e =
          if (Files.isDirectory(source)) {
            for (name ← Files.newDirectoryStream(source)) {
              val newSource = fs.getPath(source.toString, name.toString)
              val newEntryName = entryName + '/' + name
              toArchive.push((newSource, newEntryName))
            }
            new TarEntry(entryName + '/')
          }
          else {
            val e = new TarEntry(entryName)
            e.setSize(Files.size(source))
            e
          }
        e.setMode(FileUtil.permissionsToTarMode(Files.getPosixFilePermissions(source)))
        additionalCommand(e)
        tos.putNextEntry(e)
        if (!Files.isDirectory(source)) try Files.copy(source, tos) finally tos.closeEntry
      }
    }

    links.foreach {
      case (source, entryName) ⇒
        val e = new TarEntry(entryName, TarConstants.LF_SYMLINK)
        e.setLinkName(
          Files.readSymbolicLink(source).toString
        )
        e.setMode(FileUtil.permissionsToTarMode(Files.getPosixFilePermissions(source)))
        tos.putNextEntry(e)
    }
  }

}
