/*
 * Copyright (C) 2015 Romain Reuillon
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
package org.openmole.tool

import java.io._
import java.nio.channels.FileChannel
import java.nio.file._
import java.util.UUID
import java.util.concurrent.TimeoutException
import java.util.logging.Logger
import java.util.zip.{ GZIPInputStream, GZIPOutputStream, ZipFile }

import org.openmole.tool.thread._
import org.openmole.tool.lock._

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.util.{ Success, Failure, Try }
import org.openmole.tool.stream._

package file {

  import java.nio.file.DirectoryStream.Filter
  import java.nio.file.attribute.PosixFilePermissions
  import java.util.concurrent.ThreadPoolExecutor
  import java.util.concurrent.atomic.AtomicLong
  import org.openmole.tool.file
  import squants.time.Time

  import java.util

  trait FilePackage:
    p ⇒

    type File = java.io.File
    def File(s: String): File = new File(s)

    def currentDirectory = new File(".")

    val EXEC_MODE = 1 + 8 + 64
    val WRITE_MODE = 2 + 16 + 128
    val READ_MODE = 4 + 32 + 256

    def copyChannel(source: FileChannel, destination: FileChannel): Unit = source.transferTo(0, source.size, destination)

    //    def retrieveResourceFromClassLoader(file: File, clazz: Class[_], resourceName: String, executable: Boolean = false) =
    //      if (!file.exists()) {
    //        withClosable(clazz.getClassLoader.getResourceAsStream(resourceName))(_.copy(file))
    //        if (executable) file.setExecutable(true)
    //        file
    //      }

    // glad you were there...
    implicit def file2Path(file: File): Path = file.toPath

    implicit def path2File(path: Path): File = path.toFile

    implicit val fileOrdering: Ordering[File] = Ordering.by((_: File).getPath)

    implicit def predicateToFileFilter(predicate: File ⇒ Boolean): FileFilter = p1 ⇒ predicate(p1)

    def getCopyOptions(followSymlinks: Boolean) = Seq(StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING) ++
      (if (followSymlinks) Seq.empty[CopyOption] else Seq(LinkOption.NOFOLLOW_LINKS))

    implicit class FileDecorator(file: File):

      def realFile = file.toPath.toRealPath().toFile

      def realPath = file.toPath.toRealPath()

      def isDirectoryEmpty =
        file.withDirectoryStream()(_.iterator().asScala.isEmpty)

      def isEmpty =
        if (file.isDirectory) isDirectoryEmpty
        else file.size == 0L

      def listFilesSafe = Option(file.listFiles).getOrElse(Array.empty[File])
      def listFilesSafe(filter: File ⇒ Boolean) = Option(file.listFiles(filter)).getOrElse(Array.empty[File])
      def listFileSafeIterator = Files.list(file.toPath).iterator().asScala
      
      def recursiveListFilesSafe(filter: File ⇒ Boolean) = Option(file.listRecursive(filter).toArray).getOrElse(Array.empty[File])

      def getParentFileSafe: File =
        file.getParentFile() match {
          case null ⇒
            if (file.isAbsolute) file else new File(".")
          case f ⇒ f
        }

      /////// copiers ////////
      def copyContent(destination: File) = {
        val ic = new FileInputStream(file).getChannel
        try {
          val oc = new FileOutputStream(destination).getChannel
          try p.copyChannel(ic, oc)
          finally oc.close()
        }
        finally ic.close()
      }

      private def copyFile(toF: File, followSymlinks: Boolean = false) = {
        val copyOptions = getCopyOptions(followSymlinks)
        Files.copy(file, toF, copyOptions: _*)
        toF.mode = file
      }

      def copy(toF: File, followSymlinks: Boolean = false) = 
        // default options are NOFOLLOW_LINKS, COPY_ATTRIBUTES, REPLACE_EXISTING
        toF.getParentFileSafe.mkdirs()
        if Files.isDirectory(file)
        then DirUtils.copy(file, toF, followSymlinks)
        else copyFile(toF, followSymlinks)

      def copy(to: OutputStream) =
        withClosable(bufferedInputStream()):
          _.copy(to)

      // TODO replace with NIO
      def copy(to: OutputStream, maxRead: Int, timeout: Time)(implicit pool: ThreadPoolExecutor): Unit =
        withClosable(bufferedInputStream()) {
          _.copy(to, maxRead, timeout)
        }

      def copyCompressFile(toF: File): File = withClosable(new GZIPOutputStream(toF.bufferedOutputStream())) { to ⇒
        Files.copy(file, to)
        toF
      }

      def copyUncompressFile(toF: File): File = withClosable(new GZIPInputStream(file.bufferedInputStream())) { from ⇒
        Files.copy(from, toF, StandardCopyOption.REPLACE_EXISTING)
        toF
      }

      //////// modifiers ///////
      def move(to: File) = wrapError:
        to.getParentFile.mkdirs()
        def move = Files.move(file, to, StandardCopyOption.REPLACE_EXISTING)

        if !Files.isDirectory(file)
        then move
        else
          Try(move) match
            case Success(_) ⇒
            case Failure(_) ⇒ DirUtils.move(file, to)

      def forceFileDelete = wrapError:
        try Files.deleteIfExists(file)
        catch
          case t: Throwable ⇒
            FileTools.setAllPermissions(file)
            Files.deleteIfExists(file)

      def recursiveDelete: Unit = wrapError { DirUtils.deleteIfExists(file) }

      def isSymbolicLink = Files.isSymbolicLink(Paths.get(file.getAbsolutePath))

      def isBrokenSymbolicLink = Files.notExists(Files.readSymbolicLink(file))

      def directoryContainsNoFileRecursive: Boolean =
        import scala.util.boundary
        boundary:
          val toProceed = new ListBuffer[File]
          toProceed += file

          while toProceed.nonEmpty
          do
            val f = toProceed.remove(0)
            // wrap with try catch in case CARE Archive generates d--------- directories
            try
              f.withDirectoryStream(): s ⇒
                for
                  f ← s.asScala
                do
                  if Files.isRegularFile(f)
                  then boundary.break(false)
                  else if (Files.isDirectory(f)) toProceed += f
            catch
              case e: java.nio.file.AccessDeniedException ⇒ Logger.getLogger(this.getClass.getName).warning(s"Unable to browse directory ${e.getFile}")

          true

      import java.io.IOException
      import java.nio.file.FileVisitResult
      import java.nio.file.Files
      import java.nio.file.SimpleFileVisitor
      import java.nio.file.attribute.BasicFileAttributes

      //////// general operations ///////
      def baseName = file.getName.takeWhile(_ != '.')

      def size: Long =
        def sizeOfDirectory(directory: File) = {
          val size = new AtomicLong()

          Files.walkFileTree(
            directory,
            new SimpleFileVisitor[Path]() {
              override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
                if (attrs.isRegularFile) size.addAndGet(attrs.size)
                FileVisitResult.CONTINUE
              }
            })

          size.get()
        }

        if (!file.exists()) 0L
        else if (file.isDirectory) sizeOfDirectory(file)
        else
          try Files.size(file)
          catch {
            case e: NoSuchFileException ⇒ 0L
          }

      def mode =
        val f = file.realPath
        (if (Files.isReadable(f)) READ_MODE else 0) |
          (if (Files.isWritable(f)) WRITE_MODE else 0) |
          (if (Files.isExecutable(f)) EXEC_MODE else 0)

      /** set mode from an integer as retrieved from a Tar archive */
      def mode_=(m: Int) =
        val f = file.realFile
        f.setReadable((m & READ_MODE) != 0)
        f.setWritable((m & WRITE_MODE) != 0)
        f.setExecutable((m & EXEC_MODE) != 0)

      /** Copy mode from another file */
      def mode_=(other: File) =
        val f = file.realFile
        val o = other.realPath
        f.setReadable(Files.isReadable(o))
        f.setWritable(Files.isWritable(o))
        f.setExecutable(Files.isExecutable(o))

      def setPosixMode(m: String) =
        def isPosix = FileSystems.getDefault().supportedFileAttributeViews().contains("posix")

        if (isPosix) {
          val attrs = PosixFilePermissions.fromString(m)
          Files.setPosixFilePermissions(file, attrs)
          true
        }
        else false

      def content_=(content: String) =
        createParentDirectory
        Files.write(file, content.getBytes, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)

      def <(c: String) = content_=(c)

      def content: String = content()
      def content(gz: Boolean = false): String =
        if gz
        then withGzippedInputStream(is ⇒ Source.fromInputStream(is).mkString)
        else Files.readString(file.toPath)

      def append(s: String) = Files.write(file, s.getBytes, StandardOpenOption.APPEND)
      def <<(s: String) = append(s)

      def clear =
        content = ""

      def lines: IArray[String] = IArray.unsafeFromArray(Files.readAllLines(file.toPath).asScala.toArray)

      def contentOption =
        try Some(file.content)
        catch
          case e: IOException ⇒ None

      def isTextFile(bytes: Int = 10240) =
        withInputStream: is =>
          val res = Try:
            val rBytes = is.readNBytes(bytes)
            java.nio.charset.Charset.availableCharsets().get("UTF-8").newDecoder().decode(java.nio.ByteBuffer.wrap(rBytes))
          res.isSuccess

      def /(s: String): File = Paths.get(file.toString, s)

      def />(s: String): File =
        val dir = file / s
        dir.mkdirs()
        dir

      // TODO implement using NIO getLastModifiedTime
      def lastModification =
        var lastModification = file.lastModified

        if (file.isDirectory) {
          val toProceed = new ListBuffer[File]
          toProceed += file

          while (!toProceed.isEmpty) {
            val f = toProceed.remove(0)

            if (f.lastModified > lastModification) lastModification = f.lastModified

            if (f.isDirectory) {
              for (child ← f.listFilesSafe) {
                toProceed += child
              }
            }
          }
        }
        lastModification

      // TODO replace with DirectoryStream
      def listRecursive(filter: File ⇒ Boolean = _ => true) =
        val ret = new ListBuffer[File]
        applyRecursive((f: File) ⇒ if (filter(f)) ret += f)
        ret

      ///////// creation of new elements ////////
      /**
       * Create instance of temporary directory in subdirectory of caller.
       * Actual directory is NOT created yet.
       *
       * @param prefix String to prefix the generated UUID name.
       * @return New temporary directory
       */
      def newDirectory(prefix: String, create: Boolean = false): File =
        val tempDir = Paths.get(file.toString, prefix + UUID.randomUUID)
        if (create) tempDir.mkdirs()
        tempDir.toFile

        /**
       * Create instance of temporary file in directory of caller.
       * Actual file is NOT created yet.
       *
       * @param prefix String to prefix the generated UUID name.
       * @param suffix String to suffix the generated UUID name.
       * @return New temporary file
       */
      def newFile(prefix: String, suffix: String): File =
        val f = Paths.get(file.toString, prefix + UUID.randomUUID + suffix)
        f.toFile

      /**
       * Try to create a symbolic link at the calling emplacement.
       * The function creates a copy of the target file on systems not supporting symlinks.
       *
       * @param target Target of the link
       * @return
       */
      def createLinkTo(target: String): Path = createLinkTo(Paths.get(target))

      def createLinkTo(target: Path): Path =
        def unsupported =
          Logger.getLogger(getClass.getName).warning("File system doesn't support symbolic link, make a file copy instead")
          val fileTarget = if (target.isAbsolute) target else Paths.get(file.getParentFileSafe.getPath, target.getPath)
          Files.copy(fileTarget, file, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING)
          file

        try Files.createSymbolicLink(file, target)
        catch
          case _: UnsupportedOperationException ⇒ unsupported
          case e: FileAlreadyExistsException    ⇒ throw e
          case _: FileSystemException           ⇒ unsupported
          case e: IOException                   ⇒ throw e


      def createParentDirectory = wrapError {
        file.getCanonicalFile.getParentFileSafe.mkdirs
      }

      def withLock[T](f: OutputStream ⇒ T): T =
        jvmLevelFileLock.withLock(file.getCanonicalPath):
          withClosable(new FileOutputStream(file, true)) { fos ⇒
            withClosable(new BufferedOutputStream(fos)) { bfos ⇒
              val lock = fos.getChannel.lock
              try f(bfos)
              finally lock.release
            }
          }

      def withLockInDirectory[T](f: ⇒ T, lockName: String = ".lock"): T =
        file.mkdirs()
        val lockFile = file / lockName
        lockFile.createNewFile()
        try lockFile.withLock { _ ⇒ f }
        finally lockFile.delete()

      def bufferedInputStream(gz: Boolean = false) =
        if (!gz) new BufferedInputStream(Files.newInputStream(file))
        else new GZIPInputStream(new BufferedInputStream(Files.newInputStream(file)))

      private def writeOptions(append: Boolean) =
        import StandardOpenOption._
        if (append) Seq(CREATE, APPEND, WRITE) else Seq(CREATE, TRUNCATE_EXISTING, WRITE)

      def fileOutputStream = new FileOutputStream(file)

      def bufferedOutputStream(append: Boolean = false, gz: Boolean = false) =
        file.createParentDirectory
        if (!gz) new BufferedOutputStream(Files.newOutputStream(file.toPath, writeOptions(append): _*))
        else new BufferedOutputStream(Files.newOutputStream(file.toPath, writeOptions(append): _*).toGZ)

      def gzippedBufferedInputStream = new GZIPInputStream(bufferedInputStream())
      def gzippedBufferedOutputStream = new GZIPOutputStream(bufferedOutputStream())

      def withGzippedOutputStream[T] = withClosable[GZIPOutputStream, T](gzippedBufferedOutputStream)(_)
      def withGzippedInputStream[T] = withClosable[GZIPInputStream, T](gzippedBufferedInputStream)(_)

      def withOutputStream[T] = withClosable[OutputStream, T](bufferedOutputStream())(_)

      def withPrintStream[T](append: Boolean = false, create: Boolean = false, gz: Boolean = false) =
        if (create) file.createParentDirectory
        withClosable[PrintStream, T](new PrintStream(file.bufferedOutputStream(append = append, gz = gz))) _

      def atomicWithPrintStream[T](f: PrintStream ⇒ T) =
        file.createParentDirectory
        val tmpFile = java.io.File.createTempFile("stream", ".tmp", file.getParentFile)
        try {
          val printStream = new PrintStream(tmpFile.bufferedOutputStream())
          try f(printStream)
          finally printStream.close()
        }
        finally Files.move(tmpFile.toPath, file.toPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)


      def withFileOutputStream[T] = withClosable[FileOutputStream, T](fileOutputStream)(_)

      def withInputStream[T] = withClosable[InputStream, T](bufferedInputStream())(_)

      def withReader[T] = withClosable[Reader, T](Files.newBufferedReader(file.toPath))(_)

      def withWriter[T](append: Boolean = false) = withClosable[Writer, T](Files.newBufferedWriter(file.toPath, writeOptions(append = append): _*))(_)

      def withDirectoryStream[T](filter: Option[java.nio.file.DirectoryStream.Filter[Path]] = None)(f: DirectoryStream[Path] ⇒ T): T = {
        def open =
          filter match {
            case None    ⇒ Files.newDirectoryStream(file)
            case Some(f) ⇒ Files.newDirectoryStream(file, f)
          }

        val stream = open
        try f(stream)
        finally stream.close()
      }

      def withSource[T] = withClosable[Source, T](Source.fromInputStream(bufferedInputStream()))(_)

      def wrapError[T](f: ⇒ T): T =
        try f
        catch
          case t: Throwable ⇒ throw new IOException(s"For file $file", t)

      ////// synchronized operations //////
      def lockAndAppendFile(to: String): Unit = lockAndAppendFile(new File(to))

      def lockAndAppendFile(from: File): Unit = jvmLevelFileLock.withLock(file.getCanonicalPath) {
        val channelI = new FileInputStream(from).getChannel
        try {
          val channelO = new FileOutputStream(file, true).getChannel
          try {
            val lock = channelO.lock
            try p.copyChannel(channelI, channelO)
            finally lock.release
          }
          finally channelO.close
        }
        finally channelI.close
      }

      ///////// helpers ///////
      def applyRecursive(operation: File ⇒ Unit): Unit =
        applyRecursive(operation, Set.empty)

      def applyRecursive(operation: File ⇒ Unit, stopPath: Iterable[File]): Unit =
        recurse(file)(operation, stopPath)

      def isAParentOf(f: File) = f.getCanonicalPath.startsWith(file.getCanonicalPath)

      def inode: Option[Long] =
        try
          Files.getAttribute(file.toPath, "unix:ino") match
            case inode: Long => Some(inode)
            case _ => None
        catch
          case _: UnsupportedOperationException => None
          case _: IllegalArgumentException => None
          case _: NoSuchFileException => None


    private def block(file: File, stopPath: Iterable[File]) =
      stopPath.exists: f ⇒
        if f.exists() && file.exists() then Files.isSameFile(f, file) else false

    private def recurse(file: File)(operation: File ⇒ Unit, stopPath: Iterable[File]): Unit =
      if !block(file, stopPath)
      then
        def authorizeListFiles[T](f: File)(g: ⇒ T): T =
          val originalMode = f.mode
          f.setExecutable(true)
          f.setReadable(true)
          f.setWritable(true)
          try g
          finally if (f.exists) f.mode = originalMode

        if file.isDirectory && !file.isSymbolicLink
        then authorizeListFiles(file):
          for (f ← file.listFilesSafe)
          do recurse(f)(operation, stopPath)

        operation(file)

    def readableByteCount(bytes: Long): String =
      val kb = 1024L
      val mB = kb * kb
      val gB = mB * kb
      val tB = gB * kb

      val doubleBytes = bytes.toDouble
      if (bytes < mB) (doubleBytes / kb).formatted("%.2f").toString() + "KB"
      else if (bytes < gB) (doubleBytes / mB).formatted("%.2f").toString + "MB"
      else if (bytes < tB) (doubleBytes / gB).formatted("%.2f").toString + "GB"
      else (doubleBytes / tB).formatted("%.2f").toString + "TB"

    def uniqName(prefix: String, sufix: String, separator: String = "_") = prefix + separator + UUID.randomUUID.toString + sufix


    def acceptDirectory = new Filter[Path]:
      def accept(entry: Path): Boolean = Files.isDirectory(entry)




  object FileTools {
    private val allPerms =
      import java.nio.file.attribute.PosixFilePermission
      val perms = new util.HashSet[PosixFilePermission]()
      //add owners permission
      perms.add(PosixFilePermission.OWNER_READ)
      perms.add(PosixFilePermission.OWNER_WRITE)
      perms.add(PosixFilePermission.OWNER_EXECUTE)
      //add group permissions
      perms.add(PosixFilePermission.GROUP_READ)
      perms.add(PosixFilePermission.GROUP_WRITE)
      perms.add(PosixFilePermission.GROUP_EXECUTE)
      //add others permissions
      perms.add(PosixFilePermission.OTHERS_READ)
      perms.add(PosixFilePermission.OTHERS_WRITE)
      perms.add(PosixFilePermission.OTHERS_EXECUTE)
      perms

    private val isPosix =
      import java.nio.file.FileSystems
      FileSystems.getDefault.supportedFileAttributeViews.contains("posix")

    def setAllPermissions(path: Path) =
      if (isPosix) Files.setPosixFilePermissions(path, allPerms)
      else {
        val f = path.toFile
        f.setReadable(true)
        f.setWritable(true)
        f.setExecutable(true)
      }

  }

}

package object file extends FilePackage {
  val jvmLevelFileLock = new LockRepository[String]
}
