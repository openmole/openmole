/*
 * Copyright (C) 2015 Jonathan Passerat-Palmbach
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

import java.io.IOException
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import org.openmole.tool.logger.JavaLogger

class CopyDirVisitor(fromPath: Path, toPath: Path, copyOptions: Array[CopyOption]) extends SimpleFileVisitor[Path] with JavaLogger {

  // 1st one just to make it compile...
  def this(fromPath: Path, toPath: Path, copyOptions: CopyOption*) = this(fromPath, toPath, copyOptions.toArray)
  def this(fromPath: Path, toPath: Path) = this(fromPath, toPath, LinkOption.NOFOLLOW_LINKS, StandardCopyOption.COPY_ATTRIBUTES)

  @throws(classOf[IOException])
  override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {

    val targetPath = toPath.resolve(fromPath.relativize(dir))
    if (!Files.exists(targetPath)) {
      Files.createDirectory(targetPath)
    }
    FileVisitResult.CONTINUE
  }

  @throws(classOf[IOException])
  override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {

    // if this fails, we're in the case of an access denied on a *file*
    try {
      // force type inference with _*
      Files.copy(file, toPath.resolve(fromPath.relativize(file)), copyOptions *)
    }
    catch {
      case exc: java.nio.file.AccessDeniedException => Log.logger.warning(s"Could not read file ${exc.getFile} (Permission Denied), skip from copy")
    }
    FileVisitResult.CONTINUE
  }

  @throws(classOf[IOException])
  override def visitFileFailed(file: Path, exc: IOException): FileVisitResult = {
    exc match {
      // exception thrown when unable to browse a directory
      case _: java.nio.file.AccessDeniedException => Log.logger.warning(s"Could not enter directory ${file} (Permission Denied), content won't be copied")
      case _                                      => throw exc
    }
    FileVisitResult.CONTINUE
  }
}
