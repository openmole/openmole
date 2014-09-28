/** @file DirUtils.java from https://github.com/bbejeck/Java-7/blob/master/src/main/java/bbejeck/nio/util/DirUtils.java (chosen excerpt) */ 
package bbejeck.nio.util;

import bbejeck.nio.files.visitor.CopyDirVisitor;
import bbejeck.nio.files.visitor.DeleteDirVisitor;

import java.io.IOException;
import java.nio.file.*;
import java.util.EnumSet;
import java.util.Objects;

/**
 * Created by IntelliJ IDEA.
 * User: bbejeck
 * Date: 1/22/12
 * Time: 12:46 PM
 */

public class DirUtils {
  /**
   * Copies a directory tree
   * @param from
   * @param to
   * @throws IOException
   *    
   */
  public static void copy(Path from, Path to) throws IOException {
    validate(from);
    Files.walkFileTree(from, EnumSet.of(FileVisitOption.FOLLOW_LINKS),Integer.MAX_VALUE,new CopyDirVisitor(from, to));
  }

  /**
   * Moves one directory tree to another.  Not a true move operation in that the
   * directory tree is copied, then the original directory tree is deleted.
   *
   * @param from
   * @param to
   * @throws IOException
   */
  public static void move(Path from, Path to) throws IOException {
    validate(from);
    Files.walkFileTree(from, new CopyDirVisitor(from, to));
    Files.walkFileTree(from, new DeleteDirVisitor());
  }

  /**
   * Completely removes given file tree starting at and including the given path.
   *
   * @param path
   * @throws IOException
   */
  public static void delete(Path path) throws IOException {
    validate(path);
    Files.walkFileTree(path, new DeleteDirVisitor());
  }


  /**
   * If the path exists, completely removes given file tree starting at and including the given path.
   *
   * @param path
   * @throws IOException
   */
  public static void deleteIfExists(Path path) throws IOException {
    if (Files.exists(path))   delete(path);
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

