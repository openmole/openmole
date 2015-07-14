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

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration
import scala.io.Source
import scala.util.{ Success, Failure, Try }

package file {

  trait FilePackage { p ⇒

    type File = java.io.File
    def File(s: String): File = new File(s)

    def currentDirectory = new File(".")

    val DefaultBufferSize = 8 * 1024

    val EXEC_MODE = 1 + 8 + 64
    val WRITE_MODE = 2 + 16 + 128
    val READ_MODE = 4 + 32 + 256

    lazy val jvmLevelFileLock = new LockRepository[String]

    def copy(source: FileChannel, destination: FileChannel): Unit = source.transferTo(0, source.size, destination)

    // glad you were there...
    implicit def file2Path(file: File) = file.toPath

    implicit def path2File(path: Path) = path.toFile

    implicit val fileOrdering = Ordering.by((_: File).getPath)

    implicit def predicateToFileFilter(predicate: File ⇒ Boolean) = new FileFilter {
      def accept(p1: File) = predicate(p1)
    }

    implicit class OutputStreamDecorator(os: OutputStream) {
      def flushClose = {
        try os.flush
        finally os.close
      }

      def toGZ = new GZIPOutputStream(os)

      def append(content: String) = new PrintWriter(os).append(content).flush

      def appendLine(line: String) = append(line + "\n")
    }

    implicit class InputStreamDecorator(is: InputStream) {

      // FIXME useful?
      def copy(to: OutputStream): Unit = {
        val buffer = new Array[Byte](DefaultBufferSize)
        Iterator.continually(is.read(buffer)).takeWhile(_ != -1).foreach {
          to.write(buffer, 0, _)
        }
      }

      def copy(to: File, maxRead: Int, timeout: Duration): Unit =
        withClosable(to.bufferedOutputStream()) {
          copy(_, maxRead, timeout)
        }

      def copy(to: OutputStream, maxRead: Int, timeout: Duration) = {
        val buffer = new Array[Byte](maxRead)
        val executor = defaultExecutor
        val reader = new ReaderRunnable(buffer, is, maxRead)

        Iterator.continually {
          val futureRead = executor.submit(reader)

          try futureRead.get(timeout.length, timeout.unit)
          catch {
            case (e: TimeoutException) ⇒
              futureRead.cancel(true)
              throw new IOException(s"Timeout on reading $maxRead bytes, read was longer than $timeout ms.", e)
          }
        }.takeWhile(_ != -1).foreach {
          count ⇒
            val futureWrite = executor.submit(new WritterRunnable(buffer, to, count))

            try futureWrite.get(timeout.length, timeout.unit)
            catch {
              case (e: TimeoutException) ⇒
                futureWrite.cancel(true)
                throw new IOException(s"Timeout on writing $count bytes, write was longer than $timeout ms.", e)
            }
        }
      }

      def toGZ = new GZIPInputStream(is)

      // this one must have REPLACE_EXISTING enabled
      // but does not support COPY_ATTRIBUTES, nor NOFOLLOW_LINKS
      def copy(file: File) = Files.copy(is, file, StandardCopyOption.REPLACE_EXISTING)
    }

    implicit class FileDecorator(file: File) {

      def realFile = file.toPath.toRealPath().toFile

      def realPath = file.toPath.toRealPath()

      def listFilesSafe = Option(file.listFiles).getOrElse(Array.empty)

      def listFilesSafe(filter: FileFilter) = Option(file.listFiles(filter)).getOrElse(Array.empty)

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
          try p.copy(ic, oc)
          finally oc.close()
        }
        finally ic.close()
      }

      def copy(toF: File) = {
        // default options are NOFOLLOW_LINKS, COPY_ATTRIBUTES, REPLACE_EXISTING
        toF.getParentFileSafe.mkdirs()
        if (Files.isDirectory(file)) DirUtils.copy(file, toF)
        else {
          Files.copy(file, toF, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING, LinkOption.NOFOLLOW_LINKS)
          toF.mode = file
        }
      }

      def copy(to: OutputStream) = withClosable(bufferedInputStream) { _.copy(to) }

      // TODO replace with NIO
      def copy(to: OutputStream, maxRead: Int, timeout: Duration): Unit =
        withClosable(bufferedInputStream) {
          _.copy(to, maxRead, timeout)
        }

      def copyCompressFile(toF: File): File = withClosable(new GZIPOutputStream(toF.bufferedOutputStream())) { to ⇒
        Files.copy(file, to)
        toF
      }

      def copyUncompressFile(toF: File): File = withClosable(new GZIPInputStream(file.bufferedInputStream)) { from ⇒
        Files.copy(from, toF, StandardCopyOption.REPLACE_EXISTING)
        toF
      }

      //////// modifiers ///////
      def move(to: File) = wrapError {
        def move = Files.move(file, to, StandardCopyOption.REPLACE_EXISTING)
        if (!Files.isDirectory(file)) move
        else {
          Try(move) match {
            case Success(_) ⇒
            case Failure(_) ⇒ DirUtils.move(file, to)
          }
        }
      }

      def recursiveDelete: Unit = wrapError {
        def setAllPermissions(f: File) = {
          f.setReadable(true)
          f.setWritable(true)
          f.setExecutable(true)
        }
        setAllPermissions(file)

        if (!file.isSymbolicLink && file.isDirectory) {
          for (s ← file.listFilesSafe) {
            setAllPermissions(s)
            s.isDirectory match {
              case true ⇒
                s.recursiveDelete
                s.delete()
              case false ⇒ s.delete
            }
          }
        }
        file.delete()
      }

      def isJar = Try {
        val zip = new ZipFile(file)
        val hasManifestEntry =
          try zip.getEntry("META-INF/MANIFEST.MF") != null
          finally zip.close
        hasManifestEntry
      }.getOrElse(false)

      def isSymbolicLink = Files.isSymbolicLink(Paths.get(file.getAbsolutePath))

      def dirContainsNoFileRecursive: Boolean = {
        val toProceed = new ListBuffer[File]
        toProceed += file

        while (!toProceed.isEmpty) {
          val f = toProceed.remove(0)

          // wrap with try catch in case CARE Archive generates d--------- directories
          try f.withDirectoryStream(s ⇒
            for (f ← s) {
              if (Files.isRegularFile(f)) return false
              else if (Files.isDirectory(f)) toProceed += f
            })
          catch {
            case e: java.nio.file.AccessDeniedException ⇒ Logger.getLogger(this.getClass.getName).warning(s"Unable to browse directory ${e.getFile}")
          }
        }
        true
      }

      //////// general operations ///////
      def size: Long =
        if (Files.isDirectory(file)) file.withDirectoryStream(_.foldLeft(0l)((acc: Long, p: Path) ⇒ acc + p.size))
        else Files.size(file)

      def mode = {
        val f = file.realPath;
        {
          if (Files.isReadable(f)) READ_MODE else 0
        } | {
          if (Files.isWritable(f)) WRITE_MODE else 0
        } | {
          if (Files.isExecutable(f)) EXEC_MODE else 0
        }
      }

      /** set mode from an integer as retrieved from a Tar archive */
      def mode_=(m: Int) = {
        val f = file.realFile
        f.setReadable((m & READ_MODE) != 0)
        f.setWritable((m & WRITE_MODE) != 0)
        f.setExecutable((m & EXEC_MODE) != 0)
      }

      /** Copy mode from another file */
      def mode_=(other: File) = {
        val f = file.realFile
        val o = other.realPath
        f.setReadable(Files.isReadable(o))
        f.setWritable(Files.isWritable(o))
        f.setExecutable(Files.isExecutable(o))
      }

      def content_=(content: String) = Files.write(file, content.getBytes)

      def content = withSource(_.mkString)

      def contentOption =
        try Some(file.content)
        catch {
          case e: IOException ⇒ None
        }

      def /(s: String): File = Paths.get(file.toString, s)

      // TODO implement using NIO getLastModifiedTime
      def lastModification = {
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
      }

      // TODO replace with DirectoryStream
      def listRecursive(filter: File ⇒ Boolean) = {
        val ret = new ListBuffer[File]
        applyRecursive((f: File) ⇒ if (filter(f)) ret += f)
        ret
      }

      ///////// creation of new elements ////////
      /**
       * Create temporary directory in subdirectory of caller
       *
       * @param prefix String to prefix the generated UUID name.
       * @return New temporary directory
       */
      def newDir(prefix: String): File = {
        val tempDir = Paths.get(file.toString, prefix + UUID.randomUUID)
        tempDir.toFile
      }

      /**
       * Create temporary file in directory of caller
       *
       * @param prefix String to prefix the generated UUID name.
       * @param suffix String to suffix the generated UUID name.
       * @return New temporary file
       */
      def newFile(prefix: String, suffix: String): File = {
        val f = Paths.get(file.toString, prefix + UUID.randomUUID + suffix)
        f.toFile
      }

      /**
       * Try to create a symbolic link at the calling emplacement.
       * The function creates a copy of the target file on systems not supporting symlinks.
       * @param target Target of the link
       * @return
       */
      def createLink(target: String): Path = createLink(Paths.get(target))

      def createLink(target: Path): Path = {
        def unsupported = {
          Logger.getLogger(getClass.getName).warning("File system doesn't support symbolic link, make a file copy instead")
          val fileTarget = if (target.isAbsolute) target else Paths.get(file.getParentFileSafe.getPath, target.getPath)
          Files.copy(fileTarget, file, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING)
          file
        }

        try Files.createSymbolicLink(file, target)
        catch {
          case _: UnsupportedOperationException ⇒ unsupported
          case _: FileSystemException           ⇒ unsupported
          case e: IOException                   ⇒ throw e
        }
      }

      def createParentDir = wrapError {
        file.getCanonicalFile.getParentFileSafe.mkdirs
      }

      def withLock[T](f: OutputStream ⇒ T) = jvmLevelFileLock.withLock(file.getCanonicalPath) {
        withClosable(new FileOutputStream(file, true)) { fos ⇒
          withClosable(new BufferedOutputStream(fos)) { bfos ⇒
            val lock = fos.getChannel.lock
            try f(bfos)
            finally lock.release
          }
        }
      }

      def bufferedInputStream = new BufferedInputStream(new FileInputStream(file))

      def bufferedOutputStream(append: Boolean = false) = new BufferedOutputStream(new FileOutputStream(file, append))

      def gzippedBufferedInputStream = new GZIPInputStream(bufferedInputStream)

      def gzippedBufferedOutputStream = new GZIPOutputStream(bufferedOutputStream())

      def withGzippedOutputStream[T] = withClosable[GZIPOutputStream, T](gzippedBufferedOutputStream)(_)

      def withGzippedInputStream[T] = withClosable[GZIPInputStream, T](gzippedBufferedInputStream)(_)

      def withOutputStream[T] = withClosable[OutputStream, T](bufferedOutputStream())(_)

      def withInputStream[T] = withClosable[InputStream, T](bufferedInputStream)(_)

      def withWriter[T] = withClosable[Writer, T](new OutputStreamWriter(bufferedOutputStream()))(_)

      def withDirectoryStream[T] = withClosable[DirectoryStream[Path], T](Files.newDirectoryStream(file))(_)

      def withSource[T] = withClosable[Source, T](Source.fromFile(file))(_)

      def wrapError[T](f: ⇒ T): T =
        try f
        catch {
          case t: Throwable ⇒ throw new IOException(s"For file $file", t)
        }

      ////// synchronized operations //////
      def lockAndAppendFile(to: String): Unit = lockAndAppendFile(new File(to))

      def lockAndAppendFile(from: File): Unit = jvmLevelFileLock.withLock(file.getCanonicalPath) {
        val channelI = new FileInputStream(from).getChannel
        try {
          val channelO = new FileOutputStream(file, true).getChannel
          try {
            val lock = channelO.lock
            try p.copy(channelO, channelI)
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
    }

    def withClosable[C <: { def close() }, T](open: ⇒ C)(f: C ⇒ T): T = {
      val c = open
      try f(c)
      finally c.close()
    }

    private def block(file: File, stopPath: Iterable[File]) =
      stopPath.exists {
        f ⇒ if (f.exists() && file.exists()) Files.isSameFile(f, file) else false
      }

    private def recurse(file: File)(operation: File ⇒ Unit, stopPath: Iterable[File]): Unit = if (!block(file, stopPath)) {
      def authorizeListFiles[T](f: File)(g: ⇒ T): T = {
        val originalMode = f.mode
        f.setExecutable(true)
        f.setReadable(true)
        f.setWritable(true)
        try g
        finally if (f.exists) f.mode = originalMode
      }

      if (file.isDirectory && !file.isSymbolicLink) authorizeListFiles(file) {
        for (f ← file.listFilesSafe) {
          recurse(f)(operation, stopPath)
        }
      }
      operation(file)
    }

    def readableByteCount(bytes: Long): String = {
      val kb = 1024
      val mo = kb * kb
      val go = mo * kb
      val to = go * kb

      val doubleBytes = bytes.toDouble
      if (bytes < mo) {
        val ratio = doubleBytes / kb
        if (ratio < 1) ratio.formatted("%.1f") + "Ko"
        else ratio.toInt.toString() + "Ko"
      }
      else if (bytes < go) (doubleBytes / mo).toInt.toString + "Mo"
      else if (bytes < to) (doubleBytes / go).toInt.toString + "Go"
      else (doubleBytes / to).toInt.toString + "To"
    }

  }

}

package object file extends FilePackage
