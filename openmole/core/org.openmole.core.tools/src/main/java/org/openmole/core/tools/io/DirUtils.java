package org.openmole.core.tools.io;

import org.openmole.core.tools.io.visitor.CopyDirVisitor;
import org.openmole.core.tools.io.visitor.DeleteDirVisitor;

import java.io.IOException;
import java.nio.file.*;
import java.util.EnumSet;
import java.util.Set;
import java.util.Objects;


public class DirUtils {
  /**
   * Copies a directory tree
   * @param from
   * @param to
   * @throws IOException
   *    
   */
  public static void copy(Path from, Path to, Set<FileVisitOption> visitOption, CopyOption... copyOptions) throws IOException {
    validate(from);
    Files.walkFileTree(from, visitOption, Integer.MAX_VALUE, new CopyDirVisitor(from, to, copyOptions));
  }
  
  /**
   * Copies a directory tree with the following default options:
   *    - Symlinks are not followed
   *    - File attributes are copied to the new file
   *    - Overwrite destination if it exists
   * @param from Source file
   * @param to Destination path
   * @throws IOException
   *
   */
  public static void copy(Path from, Path to) throws IOException {
    copy(from, to, EnumSet.noneOf(FileVisitOption.class), LinkOption.NOFOLLOW_LINKS, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
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
  public static void move(Path from, Path to) throws IOException {
    validate(from);
    DirUtils.copy(from, to);
    DirUtils.delete(from);
  }

  /**
   *
   * Completely removes given file tree starting at and including the given path.
   *
   * @param path
   * @throws IOException
   */
  public static void delete(Path path) throws IOException {

    validate(path);

    //  TODO Shall we introduce a force option, which would force removal of hierarchy
    //  without any consideration for file permissions only when set to true?

    // make sure directory is traversable before deletion
    path.toFile().setReadable(true);
    path.toFile().setWritable(true);
    path.toFile().setExecutable(true);

    Files.walkFileTree(path, new DeleteDirVisitor());
  }

  public static void deleteIfExists(Path path) throws IOException {
    if (Files.exists(path)) delete(path);
  }

  private static void validate(Path... paths) {
    for (Path path : paths) {
      Objects.requireNonNull(path);
      if (!Files.isDirectory(path)) {
        throw new IllegalArgumentException(String.format("%s is not a directory", path.toString()));
      }
    }
  }
}
