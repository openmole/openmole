/*
 * Copyright (C) 2010 Romain Reuillon
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
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.FileSystems
import java.nio.file.Files
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.openmole.misc.exception.UserBadDataError
import scala.collection.mutable.ListBuffer
import com.ice.tar.TarInputStream
import com.ice.tar.TarOutputStream
import TarArchiver._
import java.util.logging.Logger
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import scala.io.Source
import org.openmole.misc.tools.service._

object FileUtil {

  val exec = 1 + 8 + 64
  val write = 2 + 16 + 128
  val read = 4 + 32 + 256

  lazy val vmFileLock = new LockRepository[String]

  implicit val fileOrdering = Ordering.by((_: File).getCanonicalPath)

  val DefaultBufferSize = 8 * 1024
  implicit def inputStream2InputStreamDecorator(is: InputStream) = new InputStreamDecorator(is)
  implicit def file2FileDecorator(file: File) = new FileDecorator(file)

  implicit def file2PathConverter(file: File) = file.toPath

  def copy(source: FileChannel, destination: FileChannel): Unit = destination.transferFrom(source, 0, source.size)

  class InputStreamDecorator(is: InputStream) {

    def copy(to: OutputStream): Unit = {
      val buffer = new Array[Byte](DefaultBufferSize)
      Iterator.continually(is.read(buffer)).takeWhile(_ != -1).foreach { to.write(buffer, 0, _) }
    }

    def copy(to: File, maxRead: Int, timeout: Long): Unit = {
      val os = to.bufferedOutputStream
      try copy(os, maxRead, timeout)
      finally os.close
    }

    def copy(to: OutputStream, maxRead: Int, timeout: Long) = {
      val buffer = new Array[Byte](maxRead)
      val executor = ThreadUtil.defaultExecutor
      val reader = new ReaderRunnable(buffer, is, maxRead)

      Iterator.continually {
        val futureRead = executor.submit(reader)

        try futureRead.get(timeout, TimeUnit.MILLISECONDS)
        catch {
          case (e: TimeoutException) ⇒
            futureRead.cancel(true)
            throw new IOException("Timout on reading " + maxRead + " bytes, read was longer than " + timeout + "ms.", e)
        }
      }.takeWhile(_ != -1).foreach {
        count ⇒
          val futureWrite = executor.submit(new WritterRunnable(buffer, to, count))

          try futureWrite.get(timeout, TimeUnit.MILLISECONDS)
          catch {
            case (e: TimeoutException) ⇒
              futureWrite.cancel(true)
              throw new IOException("Timeout on writting " + count + " bytes, write was longer than " + timeout + " ms.", e);
          }
      }
    }

    def toGZ = new GZIPInputStream(is)

    def copy(file: File): Unit = {
      val os = new FileOutputStream(file)
      try copy(os)
      finally os.close
    }
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

    def listRecursive(filter: FileFilter) = {
      val ret = new ListBuffer[File]
      applyRecursive((f: File) ⇒ if (filter.accept(f)) ret += f)
      ret
    }

    def mode =
      (if (file.canExecute) exec else 0) | (if (file.canWrite) write else 0) | (if (file.canRead) read else 0)

    def mode_=(m: Int) = {
      if ((m & exec) != 0) file.setExecutable(true) else file.setExecutable(false)
      if ((m & write) != 0) file.setWritable(true) else file.setWritable(false)
      if ((m & read) != 0) file.setReadable(true) else file.setReadable(false)
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

    def copy(toF: File) = {

      def goThrough(f: (File, File) ⇒ Unit) = {
        val toCopy = new ListBuffer[(File, File)]
        toCopy += ((file, toF))

        while (!toCopy.isEmpty) {
          val (curFrom, curTo) = toCopy.remove(0)
          f(curFrom, curTo)
          if (curFrom.isDirectory) {
            for (child ← curFrom.listFiles) {
              val to = new File(curTo, child.getName)
              toCopy += ((child, to))
            }
          }
        }
      }

      goThrough(
        (curFrom, curTo) ⇒
          if (curFrom.isDirectory) curTo.mkdir
          else curFrom.copyFile(curTo))

      goThrough((curFrom, curTo) ⇒ curTo.setSamePermissionsAs(curFrom))
    }

    def setSamePermissionsAs(other: File) = {
      file.setExecutable(other.canExecute)
      file.setReadable(other.canRead)
      file.setWritable(other.canWrite)
    }

    def copyFile(toF: File): Unit = {
      val from = new FileInputStream(file).getChannel

      try {
        val to = new FileOutputStream(toF).getChannel
        try FileUtil.copy(from, to) finally to.close
      }
      finally from.close
    }

    def copyCompress(toF: File): Unit = {
      if (toF.isDirectory) toF.archiveCompressDirWithRelativePathNoVariableContent(file)
      else copyCompressFile(toF)
    }

    def copyCompressFile(toF: File): Unit = {
      val to = new GZIPOutputStream(new FileOutputStream(toF))
      try file.copy(to) finally to.close
    }

    def copyUncompressFile(toF: File): Unit = {
      val from = new GZIPInputStream(new FileInputStream(file))

      try from.copy(toF)
      finally from.close
    }

    def copy(to: OutputStream): Unit = {
      val fromIS = new FileInputStream(file)
      try fromIS.copy(to) finally fromIS.close
    }

    def copy(to: OutputStream, maxRead: Int, timeout: Long) = {
      val is = bufferedInputStream
      try is.copy(to, maxRead, timeout)
      finally is.close
    }

    def move(to: File) = {
      if (!file.renameTo(to)) {
        copy(to)
        recursiveDelete
      }
    }

    def isSymbolicLink = {
      val fs = FileSystems.getDefault
      Files.isSymbolicLink(fs.getPath(file.getAbsolutePath))
    }

    def recursiveDelete: Boolean = {
      if (file.exists && file.isDirectory && !file.isSymbolicLink) {
        for (f ← file.listFiles) f.recursiveDelete
      }
      file.delete
    }

    def size: Long = {
      if (file.exists && file.isDirectory) {
        (for (f ← file.listFiles) yield f.size).sum
      }
      else file.length
    }

    def content_=(content: String) = {
      val os = new OutputStreamWriter(new FileOutputStream(file))
      try os.write(content)
      finally os.close
    }

    def content = {
      val s = Source.fromFile(file)
      try s.mkString
      finally s.close
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
      new TarInputStream(bufferedInputStream).extractDirArchiveWithRelativePathAndClose(dest)
    }

    def extractUncompressDirArchiveWithRelativePath(dest: File) =
      new TarInputStream(gzipedBufferedInputStream).extractDirArchiveWithRelativePathAndClose(dest)

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

    def createLink(target: String) = {
      val fs = FileSystems.getDefault
      val linkTo = fs.getPath(target)
      val link = fs.getPath(file.getAbsolutePath)
      try Files.createSymbolicLink(link, linkTo)
      catch {
        case e: UnsupportedOperationException ⇒
          Logger.getLogger(FileUtil.getClass.getName).warning("File system doesn't support symbolic link, make a file copy instead")
          val targetFile = new File(target)
          if (targetFile.isAbsolute) targetFile.copy(file)
          else new File(file.getParentFile, target).copy(file)
      }
    }

    def createParentDir = {
      val parent = file.getCanonicalFile.getParentFile
      if (parent != null) {
        if (!parent.exists) parent.mkdirs
        if (!parent.isDirectory) throw new UserBadDataError("Cannot create directory " + file.getParentFile)
      }
    }

    def child(f: File): File = child(f.getPath)
    def child(s: String): File = new File(file, s)

  }

  implicit def toFileFilterConverter(f: File ⇒ Boolean) = new FileFilter {
    override def accept(file: File) = f(file)
  }

}

