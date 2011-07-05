/*
 * Copyright (C) 2010 reuillon
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
import java.nio.channels.FileChannel
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import scala.collection.mutable.ListBuffer
import com.ice.tar.TarInputStream
import com.ice.tar.TarOutputStream
import TarArchiver._
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import scala.io.Source

object FileUtil {

  implicit val fileOrdering = new Ordering[File] {
    override def compare(left: File, right: File) = {
      left.getAbsolutePath.compareTo(right.getAbsolutePath)
    }
  }
  
  val DefaultBufferSize = 8 * 1024

  def copy(source: FileChannel, destination: FileChannel): Unit = destination.transferFrom(source, 0, source.size)
  implicit def inputStreamDecorator(is: InputStream) = new {
    def copy(to: OutputStream): Unit = {
      val buffer = new Array[Byte](DefaultBufferSize)
      Stream.continually(is.read(buffer)).takeWhile(_ != -1).foreach{ 
        count => to.write(buffer, 0, count)
      }
    }

    def copy(to: OutputStream, maxRead: Int, timeout: Long) = {
      val buffer = new Array[Byte](maxRead)
      val executor = Executors.newSingleThreadExecutor
      val reader = new ReaderRunnable(buffer, is, maxRead)
    
      Stream.continually(
        {
          val futureRead = executor.submit(reader)
            
          try {
            futureRead.get(timeout, TimeUnit.MILLISECONDS)
          } catch {
            case (e: TimeoutException) =>
              futureRead.cancel(true)
              throw new IOException("Timout on reading " + maxRead + " bytes, read was longer than " + timeout + "ms.", e)
          }
        }).takeWhile(_ != -1).foreach{ 
        count => 
                
        val futureWrite = executor.submit(new WritterRunnable(buffer, to, count))

        try {
          futureWrite.get(timeout, TimeUnit.MILLISECONDS)
        } catch  {
          case (e: TimeoutException) =>
            futureWrite.cancel(true)
            throw new IOException("Timeout on writting " + count + " bytes, write was longer than " + timeout + " ms.", e);
        } 
      }     
    }

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
  }
  
  implicit def file2FileDecorator(file: File) = new FileDecorator(file)
  
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
            for (child <- f.listFiles) {
              toProceed += child
            }
          }
        }
      }
      lastModification
    }
    
    def listRecursive(filter: FileFilter) = {
      val ret = new ListBuffer[File]
      applyRecursive((f: File) => if(filter.accept(f)) ret += f)
      ret
    }
    
    def applyRecursive(operation: File => Unit): Unit = 
      applyRecursive(operation, Set.empty)

    def applyRecursive(operation: File => Unit, stopPath: Set[File]): Unit = {
      val toProceed = new ListBuffer[File]
      toProceed += file

      while (!toProceed.isEmpty) {
        val f = toProceed.remove(0)
        if (!stopPath.contains(f)) {
          operation(f)
          if (f.isDirectory) {
            for (child <- f.listFiles)  toProceed += child
          }
        }
      }
    }
    
    def dirContainsNoFileRecursive: Boolean = {
      val toProceed = new ListBuffer[File]
      toProceed += file

      while (!toProceed.isEmpty) {
        val f = toProceed.remove(0)
        for (sub <- f.listFiles) {
          if (sub.isFile) return false
          else if (sub.isDirectory) toProceed += sub
        }
      }
      true
    }

    def copy(toF: File) = {
      val toCopy = new ListBuffer[(File, File)]
      toCopy += ((file, toF))

      while (!toCopy.isEmpty) {
        val cur = toCopy.remove(0)
        val curFrom = cur._1
        val curTo = cur._2
        if (curFrom.isDirectory) {
          curTo.mkdir

          for (child <- curFrom.listFiles) {
            val to = new File(curTo, child.getName)
            toCopy += ((child, to))
          }
        } else curFrom.copyFile(curTo)
      }
    }
    
    def copyFile(toF: File): Unit = {
      val from = new FileInputStream(file).getChannel

      try {
        val to = new FileOutputStream(toF).getChannel
        try FileUtil.copy(from, to) finally to.close
      } finally from.close
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
    
    def move(to: File) = {
      if (!file.renameTo(to)) {
        copy(to)
        recursiveDelete
      }
    }
  
    def recursiveDelete: Boolean = {
      if(file.exists && file.isDirectory) {		
        for(f <- file.listFiles) f.recursiveDelete
      }
      file.delete
    }
    
    def size: Long = {
      if(file.exists && file.isDirectory) {
        (for(f <- file.listFiles) yield f.size).sum
      } else file.length
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
    
    def bufferedInputStream = new BufferedInputStream(new FileInputStream(file))
    
    def bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file))
      
    def gzipedBufferedInputStream = new GZIPInputStream(bufferedInputStream)
    
    def gzipedBufferedOutputStream = new GZIPOutputStream(bufferedOutputStream)
    
    def archiveDirWithRelativePathNoVariableContent(dest: File) = {
      val os = new TarOutputStream(new FileOutputStream(file))
      try os.createDirArchiveWithRelativePathNoVariableContent(dest)
      finally os.close
    }
  
    def archiveCompressDirWithRelativePathNoVariableContent(dest: File) = {
      val os = new TarOutputStream(gzipedBufferedOutputStream)
      try os.createDirArchiveWithRelativePathNoVariableContent(dest)
      finally os.close
    }
    
    def extractDirArchiveWithRelativePath(dest: File) = {
      new TarInputStream(gzipedBufferedInputStream).extractDirArchiveWithRelativePathAndClose(dest)
    }  
    
    def extractUncompressDirArchiveWithRelativePath(dest: File) =
      new TarInputStream(gzipedBufferedInputStream).extractDirArchiveWithRelativePathAndClose(dest)
    
  }


 
}


