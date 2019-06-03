package org.openmole.tool.file

import java.io.IOException
import java.nio.file._
import java.util.{ EnumSet, Set, Objects }

object DirUtils {
  /**
   * Copies a directory tree
   *
   * @param from
   * @param to
   * @throws IOException
   *
   */
  @throws[IOException]
  def copy(from: Path, to: Path, visitOption: Set[FileVisitOption], copyOptions: Array[CopyOption]): Path = {
    validate(from)
    Files.walkFileTree(from, visitOption, Integer.MAX_VALUE, new CopyDirVisitor(from, to, copyOptions))
  }

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
  def copy(from: Path, to: Path, followSymlinks: Boolean = false): Path = {
    val copyOptions = getCopyOptions(followSymlinks)
    copy(from, to, EnumSet.noneOf(classOf[FileVisitOption]), copyOptions.toArray)
  }

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
  def move(from: Path, to: Path): Path = {
    validate(from)
    DirUtils.copy(from, to)
    DirUtils.delete(from)
    to
  }

  /**
   *
   * Completely removes given file tree starting at and including the given path.
   *
   * @param path
   * @throws IOException
   */
  @throws[IOException]
  def delete(path: Path) = {
    validate(path)
    //  TODO Shall we introduce a force option, which would force removal of hierarchy
    //  without any consideration for file permissions only when set to true?
    // make sure directory is traversable before deletion
    path.toFile.setReadable(true)
    path.toFile.setWritable(true)
    path.toFile.setExecutable(true)
    Files.walkFileTree(path, new DeleteDirVisitor)
  }

  @throws[IOException]
  def deleteIfExists(path: Path) = {
    if (Files.exists(path)) delete(path)
  }

  private def validate(paths: Path*) = {
    for (path ← paths) {
      Objects.requireNonNull(path)
      if (!Files.isDirectory(path)) throw new IllegalArgumentException(s"${path.toString} is not a directory")
    }
  }
}
