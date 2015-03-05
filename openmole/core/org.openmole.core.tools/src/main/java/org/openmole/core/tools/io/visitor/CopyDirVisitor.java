package org.openmole.core.tools.io.visitor;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;


public class CopyDirVisitor extends SimpleFileVisitor<Path> {

  private Path fromPath;
  private Path toPath;
  private CopyOption[] copyOptions;


  public CopyDirVisitor(Path fromPath, Path toPath, CopyOption... copyOptions) {
    this.fromPath = fromPath;
    this.toPath = toPath;
    this.copyOptions = copyOptions;
  }

  public CopyDirVisitor(Path fromPath, Path toPath) {
    this(fromPath, toPath, LinkOption.NOFOLLOW_LINKS, StandardCopyOption.COPY_ATTRIBUTES);
  }

  @Override
  public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

    Path targetPath = toPath.resolve(fromPath.relativize(dir));
    if(!Files.exists(targetPath)){
      Files.createDirectory(targetPath);
    }
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

    Files.copy(file, toPath.resolve(fromPath.relativize(file)), copyOptions);
    return FileVisitResult.CONTINUE;
  }
}

