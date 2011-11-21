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

import com.ice.tar.TarEntry
import com.ice.tar.TarInputStream
import com.ice.tar.TarOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Stack
import org.openmole.misc.tools.io.FileUtil._
import java.nio.file.FileSystems
import java.nio.file.Files

object TarArchiver {
  
  implicit def TarInputStream2TarInputStreamDecorator(tis: TarInputStream) = new TarInputStreamDecorator(tis)
  implicit def TarOutputStream2TarOutputStreamComplement(tos: TarOutputStream) = new TarOutputStreamDecorator(tos)


  class TarOutputStreamDecorator(tos: TarOutputStream) {
 
    def addFile(f: File, name: String) = {
      val entry = new TarEntry(name)
      entry.setSize(f.length)
      entry.setMode(f.mode)
      tos.putNextEntry(entry)
      try f.copy(tos) finally tos.closeEntry
    }
    
    def createDirArchiveWithRelativePathNoVariableContent(baseDir: File) = createDirArchiveWithRelativePathWithAdditionnalCommand(tos, baseDir, (e:TarEntry) => e.setModTime(0))
    def createDirArchiveWithRelativePath(baseDir: File) = createDirArchiveWithRelativePathWithAdditionnalCommand(tos, baseDir, {(e)=>})
  }
  
  class TarInputStreamDecorator(tis: TarInputStream) {
    
    def applyAndClose[T](f: TarEntry => T): Iterable[T] = try {
      val ret = new ListBuffer[T]
    
      var e = tis.getNextEntry
      while(e != null) {
        ret += f(e)
        e = tis.getNextEntry
      }
      ret
    } finally tis.close
    
    def extractDirArchiveWithRelativePathAndClose(baseDir: File) = try {
      if (!baseDir.isDirectory) throw new IOException(baseDir.getAbsolutePath + " is not a directory.")
      val fs = FileSystems.getDefault
      
      var e = tis.getNextEntry

      while(e != null) {
        val dest = new File(baseDir, e.getName)
        if(!e.getLinkName.isEmpty) {
          dest.createLink(e.getLinkName)
        }
        else if(e.isDirectory) {
          dest.mkdirs
        } else {
            dest.getParentFile.mkdirs
            val fos = new FileOutputStream(dest)
            try tis.copy(fos) finally fos.close
          }
        dest.mode = e.getMode
        e = tis.getNextEntry
      }
    } finally tis.close
  }

  private def createDirArchiveWithRelativePathWithAdditionnalCommand(tos: TarOutputStream, baseDir: File, additionnalCommand: TarEntry => Unit ) = {
    if (!baseDir.isDirectory) throw new IOException(baseDir.getAbsolutePath + " is not a directory.")

    val fs = FileSystems.getDefault 
    val toArchive = new Stack[(File, String)]
    toArchive.push((baseDir, ""))

    var links = List.empty[(File, String)]
    
    while (!toArchive.isEmpty) {
      val cur = toArchive.pop
      if(Files.isSymbolicLink(fs.getPath(cur._1.getAbsolutePath)))  links ::= cur._1 -> cur._2
      else {       
        val e =  if (cur._1.isDirectory) {
          for (name <- cur._1.list.sorted) toArchive.push((new File(cur._1, name), cur._2  + '/' + name))
          new TarEntry(cur._2 + '/')
        } else {
          val e = new TarEntry(cur._2)
          e.setSize(cur._1.length)
          e
        }

        e.setMode(cur._1.mode)
        additionnalCommand(e)
        tos.putNextEntry(e)
        if (!cur._1.isDirectory) try cur._1.copy(tos) finally tos.closeEntry
      }
    }
    
    links.foreach {
      cur =>
      val e = new TarEntry(cur._2)
      e.setLinkName(fs.getPath(cur._1.getParentFile.getCanonicalPath).relativize(fs.getPath(cur._1.getCanonicalPath)).toString)
      e.setMode(cur._1.mode)
      tos.putNextEntry(e)
    }
  }
  
}
