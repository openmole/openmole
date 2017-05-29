/*
 * Copyright (C) 2014 Romain Reuillon
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

package org.openmole.tool.file

import java.io.{ IOException }
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{ FileVisitResult, Files, Path, SimpleFileVisitor }

import scala.collection.JavaConverters._

object DeleteDirVisitor {
  def setAllPermissions(f: File) = {
    f.setReadable(true)
    f.setWritable(true)
    f.setExecutable(true)
  }
}

class DeleteDirVisitor extends SimpleFileVisitor[Path] {

  override def preVisitDirectory(dir: Path, attr: BasicFileAttributes) = {
    // make sure direct sub-directories are traversable before deletion
    dir.toFile.withDirectoryStream(Some(acceptDirectory)) {
      _.asScala.foreach(f ⇒ DeleteDirVisitor.setAllPermissions(f.toFile))
    }

    FileVisitResult.CONTINUE
  }

  override def visitFile(file: Path, attrs: BasicFileAttributes) = {
    Files.delete(file)
    FileVisitResult.CONTINUE
  }

  override def postVisitDirectory(dir: Path, exc: IOException) = {
    if (exc == null) {
      dir.toFile.withDirectoryStream() {
        _.asScala.foreach { f ⇒ DeleteDirVisitor.setAllPermissions(f.toFile); f.delete }
      }

      Files.delete(dir)
      FileVisitResult.CONTINUE
    }
    throw exc
  }
}
