/*
 * Copyright (C) 2010 Romain Reuillon
 * Copyright (C) 2014 Jonathan Passerat-Palmbach
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

package org.openmole.misc.tools.io

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileFilter
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.io.Writer

import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipFile

import java.nio.channels.FileChannel
import java.nio.file.{ Path, Paths, Files, StandardCopyOption, LinkOption }
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermission._
import bbejeck.nio.util.DirUtils

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

import scala.collection.mutable.ListBuffer

import com.ice.tar.TarInputStream
import com.ice.tar.TarOutputStream
import TarArchiver._

import java.util.logging.Logger

import scala.concurrent.duration.Duration
import org.openmole.misc.tools.service._
import scala.util.{ Try, Failure, Success }

import scala.collection.JavaConversions._ // provide scala foreach over Java collections
import scala.collection.JavaConverters._ // convert Java Set to Scala

object FileUtil {

  implicit def javaSet2ScalaSet(javaSet: java.util.Set[PosixFilePermission]) = (javaSet asScala) toSet

  val TAR_EXEC = 1 + 8 + 64
  val TAR_WRITE = 2 + 16 + 128
  val TAR_READ = 4 + 32 + 256

  /** Replace the now deprecated FileUtil.mode function */
  def permissionsToTarMode(inPermissions: Set[PosixFilePermission]): Int = {
    import PosixFilePermission._

    { if (inPermissions contains (OWNER_EXECUTE)) TAR_EXEC else 0 } |
      { if (inPermissions contains (OWNER_READ)) TAR_READ else 0 } |
      { if (inPermissions contains (OWNER_WRITE)) TAR_WRITE else 0 }

  }

  lazy val vmFileLock = new LockRepository[String]

  implicit val fileOrdering = Ordering.by((_: File).getCanonicalPath)

  val DefaultBufferSize = 8 * 1024
  implicit def inputStream2InputStreamDecorator(is: InputStream) = new InputStreamDecorator(is)
  implicit def file2FileDecorator(file: File) = new FileDecorator(file)
  implicit def predicateToFileFilter(predicate: File ⇒ Boolean) = new FileFilter {
    def accept(p1: File) = predicate(p1)
  }

  // glad you were there...
  implicit def file2Path(file: File) = file.toPath
  implicit def path2File(path: Path) = path.toFile

  def copy(source: FileChannel, destination: FileChannel): Unit = destination.transferFrom(source, 0, source.size)

  class InputStreamDecorator(is: InputStream) {

    // FIXME useful?
    def copy(to: OutputStream): Unit = {
      val buffer = new Array[Byte](DefaultBufferSize)
      Iterator.continually(is.read(buffer)).takeWhile(_ != -1).foreach { to.write(buffer, 0, _) }
    }

    def copy(to: File, maxRead: Int, timeout: Duration): Unit = {
      val os = to.bufferedOutputStream
      try copy(os, maxRead, timeout)
      finally os.close
    }

    def copy(to: OutputStream, maxRead: Int, timeout: Duration) = {
      val buffer = new Array[Byte](maxRead)
      val executor = ThreadUtil.defaultExecutor
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

    // new version using NIO
    def copy(file: Path) = Files.copy(is, file, StandardCopyOption.COPY_ATTRIBUTES, LinkOption.NOFOLLOW_LINKS)
  }

  implicit def outputStreamDecorator(os: OutputStream) = new {
    def flushClose = {
      try os.flush
      finally os.close
    }

    def toGZ = new GZIPOutputStream(os)

    def append(content: String) = new PrintWriter(os).append(content).flush
    def appendLine(line: String) = append(line + "\n")
  }

  class FileDecorator(file: File) {

    // TODO reimplement using NIO getLastModifiedTime
    def lastModification = {
      var lastModification = file.lastModified

      if (file.isDirectory) {
        val toProceed = new ListBuffer[File]
        toProceed += file

        while (!toProceed.isEmpty) {
          val f = toProceed.remove(0)

          if (f.lastModified > lastModification) lastModification = f.lastModified

          if (f.isDirectory) {
            for (child ← f.listFiles) {
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

    // new version using NIO
    def mode =
      permissionsToTarMode(Files.getPosixFilePermissions(file))

    // new version using NIO
    def mode_=(m: Int) = {
      var permSet: Set[PosixFilePermission] = Set()

      if ((m & TAR_EXEC) != 0) permSet += OWNER_EXECUTE
      if ((m & TAR_WRITE) != 0) permSet += OWNER_WRITE
      if ((m & TAR_READ) != 0) permSet += OWNER_READ
      Files.setPosixFilePermissions(file, permSet)
    }

    def recursiveSize = {
      var size = 0L
      applyRecursive((f: File) ⇒ size += f.length)
      size
    }

    def applyRecursive(operation: File ⇒ Unit): Unit =
      applyRecursive(operation, Set.empty)

    def applyRecursive(operation: File ⇒ Unit, stopPath: Set[File], followSymLinks: Boolean = true): Unit = {
      val toProceed = new ListBuffer[File]
      toProceed += file

      while (!toProceed.isEmpty) {
        val f = toProceed.remove(0)
        if (!stopPath.contains(f)) {
          operation(f)
          if (f.isDirectory && (followSymLinks && !f.isSymbolicLink)) {
            for (child ← f.listFiles) toProceed += child
          }
        }
      }
    }

    // TODO replace with NIO
    def dirContainsNoFileRecursive: Boolean = {
      val toProceed = new ListBuffer[File]
      toProceed += file

      while (!toProceed.isEmpty) {
        val f = toProceed.remove(0)
        for (sub ← f.listFiles) {
          if (sub.isFile) return false
          else if (sub.isDirectory) toProceed += sub
        }
      }
      true
    }

    // new version using NIO
    def copy(toF: File) = {

      // default options are NOFOLLOW_LINKS, COPY_ATTRIBUTES
      if (Files.isDirectory(file)) DirUtils.copy(file, toF)
      else Files.copy(file, toF, StandardCopyOption.COPY_ATTRIBUTES, LinkOption.NOFOLLOW_LINKS)
    }

    def copyCompress(toF: File): File = {
      if (toF.isDirectory) toF.archiveCompressDirWithRelativePathNoVariableContent(file)
      else copyCompressFile(toF)
      toF
    }

    // new version using NIO
    def copyCompressFile(toF: File): File = {
      val to = new GZIPOutputStream(toF.bufferedOutputStream)

      Files.copy(file, to)
      toF
    }

    // new version using NIO
    def copyUncompressFile(toF: File): File = {
      val from = new GZIPInputStream(file.bufferedInputStream)

      Files.copy(from, toF, StandardCopyOption.COPY_ATTRIBUTES, LinkOption.NOFOLLOW_LINKS)
      toF
    }

    // new version with NIO
    def copyFile(toF: File) = Files.copy(file, toF, StandardCopyOption.COPY_ATTRIBUTES, LinkOption.NOFOLLOW_LINKS)

    // new version with NIO
    def copy(to: OutputStream) = Files.copy(file, to)

    // TODO replace with NIO
    def copy(to: OutputStream, maxRead: Int, timeout: Duration): Unit = {
      val is = bufferedInputStream
      try is.copy(to, maxRead, timeout)
      finally is.close
    }

    // new version using NIO
    def move(to: Path) = {
      if (!Files.isDirectory(file)) Files.move(file, to)
      else DirUtils.move(file, to)
    }

    // new version using NIO
    def isSymbolicLink = Files.isSymbolicLink(Paths.get(file.getAbsolutePath))

    // new version using NIO
    def isEmpty =
      if (Files.notExists(file)) true
      else if (!Files.isDirectory(file)) file.size == 0
      else isDirectoryEmpty(file)

    // new version using NIO
    def recursiveDelete = {
      DirUtils.deleteIfExists(file)
    }

    // TODO reimplement using NIO
    def size: Long = {
      if (file.exists && file.isDirectory) {
        (for (f ← file.listFiles) yield f.size).sum
      }
      else file.length
    }

    // new version using NIO
    def content_=(content: String) = Files.write(file, content.getBytes)

    // new version using NIO
    def content = {
      val s = Files.readAllLines(file, java.nio.charset.Charset.defaultCharset)
      s.mkString
    }

    def contentOption =
      try Some(file.content)
      catch {
        case e: IOException ⇒ None
      }

    def bufferedInputStream = new BufferedInputStream(new FileInputStream(file))
    def bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file))

    def gzipedBufferedInputStream = new GZIPInputStream(bufferedInputStream)
    def gzipedBufferedOutputStream = new GZIPOutputStream(bufferedOutputStream)

    def withOutputStream[T](f: OutputStream ⇒ T) = {
      val os = bufferedOutputStream
      try f(os)
      finally os.close
    }

    def withInputStream[T](f: InputStream ⇒ T) = {
      val is = bufferedInputStream
      try f(is)
      finally is.close
    }

    def withWriter[T](f: Writer ⇒ T): T = {
      val w = new OutputStreamWriter(bufferedOutputStream)
      try f(w)
      finally w.close
    }

    def archiveDirWithRelativePathNoVariableContent(toArchive: File) = {
      val os = new TarOutputStream(new FileOutputStream(file))
      try os.createDirArchiveWithRelativePathNoVariableContent(toArchive)
      finally os.close
    }

    //FIXME method name is ambiguous rename
    def archiveCompressDirWithRelativePathNoVariableContent(dest: File) = {
      val os = new TarOutputStream(gzipedBufferedOutputStream)
      try os.createDirArchiveWithRelativePathNoVariableContent(dest)
      finally os.close
    }

    def extractDirArchiveWithRelativePath(dest: File) = {
      val is = new TarInputStream(bufferedInputStream)
      try is.extractDirArchiveWithRelativePath(dest)
      finally is.close
    }

    def extractUncompressDirArchiveWithRelativePath(dest: File) = {
      val is = new TarInputStream(gzipedBufferedInputStream)
      try is.extractDirArchiveWithRelativePath(dest)
      finally is.close
    }

    def withLock[T](f: OutputStream ⇒ T) = vmFileLock.withLock(file.getCanonicalPath) {
      val fos = new FileOutputStream(file, true)
      val bfos = new BufferedOutputStream(fos)
      try {
        val lock = fos.getChannel.lock
        try f(bfos)
        finally lock.release
      }
      finally bfos.close
    }

    def lockAndAppendFile(to: String): Unit = lockAndAppendFile(new File(to))

    def lockAndAppendFile(from: File): Unit = vmFileLock.withLock(file.getCanonicalPath) {
      val channelI = new FileInputStream(from).getChannel
      try {
        val channelO = new FileOutputStream(file, true).getChannel
        try {
          val lock = channelO.lock
          try FileUtil.copy(channelO, channelI)
          finally lock.release
        }
        finally channelO.close
      }
      finally channelI.close
    }

    // new version using NIO
    /**
     * Try to create a symbolic link at the calling emplacement.
     * The function creates a copy of the target file on systems not supporting symlinks.
     * @param target Target of the link
     * @return
     */
    def createLink(target: String) = {

      val linkTarget = Paths.get(target)
      try Files.createSymbolicLink(file, linkTarget)
      catch {
        case e: UnsupportedOperationException ⇒ {
          Logger.getLogger(FileUtil.getClass.getName).warning("File system doesn't support symbolic link, make a file copy instead")
          Files.copy(file, linkTarget, StandardCopyOption.COPY_ATTRIBUTES, LinkOption.NOFOLLOW_LINKS)
        }
      }
    }

    // new version using NIO
    def createParentDir = wrapError {
      Files.createDirectories(file.toPath.getParent)
    }

    // new version using NIO
    def child(s: String): File = Paths.get(file.toString, s)

    def wrapError[T](f: ⇒ T): T =
      try f
      catch {
        case t: Throwable ⇒ throw new IOException(s"For file $file", t)
      }

    def updateIfTooOld(
      tooOld: Duration,
      timeStamp: File ⇒ File = f ⇒ new File(file.getPath + "-timestamp"),
      updating: File ⇒ File = f ⇒ new File(file.getPath + "-updating"))(update: File ⇒ Unit) = {
      val upFile = updating(file)
      val otherUpdating = !upFile.createNewFile

      try {
        if (!otherUpdating) {
          val ts = timeStamp(file)
          val upToDate =
            if (!file.exists || !ts.exists) false
            else
              Try(ts.content.toLong) match {
                case Success(v) ⇒ v + tooOld.toMillis > System.currentTimeMillis
                case Failure(_) ⇒ ts.delete; false
              }

          if (!upToDate) {
            update(file)
            ts.content = System.currentTimeMillis.toString
          }

        }
      }
      finally upFile.delete
      file
    }

    // new version using NIO
    // TODO get rid of toFile
    /**
     * Create temporary directory
     *
     * @param prefix String to prefix the generated UUID name.
     * @return Newly created temporary directory
     */
    def newDir(prefix: String): File = Files.createTempDirectory(prefix).toFile

    // new version using NIO
    // TODO get rid of toFile
    /**
     * Create temporary file
     *
     * @param prefix String to prefix the generated UUID name.
     * @param suffix String to suffix the generated UUID name.
     * @return Newly created temporary file
     */
    def newFile(prefix: String, suffix: String): File = Files.createTempFile(prefix, suffix).toFile

    def isJar = Try {
      val zip = new ZipFile(file)
      val hasManifestEntry =
        try zip.getEntry("META-INF/MANIFEST.MF") != null
        finally zip.close
      hasManifestEntry
    }.getOrElse(false)

  }

  def isDirectoryEmpty(d: Path) = Files.newDirectoryStream(d).iterator.hasNext
}
