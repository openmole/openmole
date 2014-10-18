package org.openmole.misc.tools.io.visitor;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;


public class DeleteDirVisitor  extends SimpleFileVisitor<Path> {

  @Override
   public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
     throws IOException {

     // make sure directory will be browsable before deletion
     dir.toFile().setReadable(true);
     dir.toFile().setWritable(true);
     dir.toFile().setExecutable(true);

     return FileVisitResult.CONTINUE;
  }

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

