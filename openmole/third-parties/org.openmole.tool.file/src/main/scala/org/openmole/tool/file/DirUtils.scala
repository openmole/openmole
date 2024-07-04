package org.openmole.tool.file

import java.io.IOException
import java.nio.file.{CopyOption, FileVisitOption, Files, Path}
import java.util.{EnumSet, Objects, Set}
import scala.annotation.tailrec

/*
 * Copyright (C) 2021 Romain Reuillon
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

object DirUtils:
  /**
   * Copies a directory tree
   *
   * @param from
   * @param to
   * @throws IOException
   *
   */
  @throws[IOException]
  def copy(from: Path, to: Path, visitOption: Set[FileVisitOption], copyOptions: Array[CopyOption]): Path =
    validate(from)
    Files.walkFileTree(from, visitOption, Integer.MAX_VALUE, new CopyDirVisitor(from, to, copyOptions))

  /**
   * Copies a directory tree with the following default options:
   *    - Symlinks are not followed
   *    - File attributes are copied to the new file
   *    - Overwrite destination if it exists
   *
   * @param from Source file
   * @param to   Destination path
   * @throws IOException
   *
   */
  @throws[IOException]
  def copy(from: Path, to: Path, followSymlinks: Boolean = false): Path =
    val copyOptions = getCopyOptions(followSymlinks)
    copy(from, to, EnumSet.noneOf(classOf[FileVisitOption]), copyOptions.toArray)

  /**
   * Moves one directory tree to another.  Not a true move operation in that the
   * directory tree is copied, then the original directory tree is deleted.
   * Reuse the default copy options from DirUtils.copy
   *
   * @param from
   * @param to
   * @throws IOException
   * @see DirUtils.copy
   */
  @throws[IOException]
  def move(from: Path, to: Path): Path =
    validate(from)
    DirUtils.copy(from, to)
    DirUtils.delete(from)
    to

  /**
   *
   * Completely removes given file tree starting at and including the given path.
   *
   * @param path
   * @throws IOException
   */
  @throws[IOException]
  def delete(p: Path): Unit =
    val file = p.toFile
    if !file.toFile.isSymbolicLink && file.isDirectory
    then
      val list =
        try file.listFilesSafe
        catch
          case t: IOException ⇒
            FileTools.setAllPermissions(file)
            file.listFilesSafe

      for
        s ← list
      do
        if s.isDirectory
        then delete(s.toPath)
        else s.forceFileDelete

    file.forceFileDelete

  @throws[IOException]
  def deleteIfExists(path: Path) =
    if Files.exists(path) then delete(path)

  private def validate(paths: Path*) =
    for
      path ← paths
    do
      Objects.requireNonNull(path)
      if (!Files.isDirectory(path)) throw new IllegalArgumentException(s"${path.toString} is not a directory")

