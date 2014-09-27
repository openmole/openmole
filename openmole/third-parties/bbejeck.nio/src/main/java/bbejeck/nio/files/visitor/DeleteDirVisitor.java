package bbejeck.nio.files.visitor;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 *  * Created by IntelliJ IDEA.
 * User: bbejeck
 * Date: 1/23/12
 * Time: 10:16 PM
 */

public class DeleteDirVisitor  extends SimpleFileVisitor<Path> {

  @Override
  public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
    Files.delete(file);
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
    if(exc == null){
      Files.delete(dir);
      return FileVisitResult.CONTINUE;
    }
    throw exc;
  }
}

