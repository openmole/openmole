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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.commons.tools.io

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import scala.collection.mutable.Stack

object TarArchiver extends IArchiver {

  override def createDirArchiveWithRelativePathNoVariableContent(baseDir: File, archive: OutputStream) = {
    createDirArchiveWithRelativePathWithAdditionnalCommand(baseDir, archive, (e:TarArchiveEntry) => e.setModTime(0))
  }

  override def createDirArchiveWithRelativePath(baseDir: File, archive: OutputStream) = {
    createDirArchiveWithRelativePathWithAdditionnalCommand(baseDir, archive, {(e)=>})
  }

  private def createDirArchiveWithRelativePathWithAdditionnalCommand(baseDir: File, archive: OutputStream, additionnalCommand: TarArchiveEntry => Unit ) = {
    if (!baseDir.isDirectory) {
      throw new IOException(baseDir.getAbsolutePath + " is not a directory.")
    }

    val tos = new TarArchiveOutputStream(archive)
    tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)

    try {
      val toArchive = new Stack[(File, String)]
      toArchive.push((baseDir, ""))

      while (!toArchive.isEmpty) {
        val cur = toArchive.pop

        if (cur._1.isDirectory) {
          for (name <- cur._1.list.sorted) {
            toArchive.push((new File(cur._1, name), cur._2 + '/' + name))
          }
        } else {
          val e = new TarArchiveEntry(cur._2)
          e.setSize(cur._1.length)
          additionnalCommand(e)
          tos.putArchiveEntry(e)
          try {
            val fis = new FileInputStream(cur._1)
            try {
              FileUtil.copy(fis, tos)
            } finally {
              fis.close
            }
          } finally {
            tos.closeArchiveEntry
          }
        }
      }
    } finally {
      tos.close
    }
  }


  override def extractDirArchiveWithRelativePath(baseDir: File, archive: InputStream) = {
    if (!baseDir.isDirectory) {
      throw new IOException(baseDir.getAbsolutePath + " is not a directory.")
    }
    val tis = new TarArchiveInputStream(archive)

    try {
      var e = tis.getNextTarEntry
      while (e != null) {
        val dest = new File(baseDir, e.getName)
        dest.getParentFile.mkdirs
        val fos = new FileOutputStream(dest)
        try {
          FileUtil.copy(tis, fos)
        } finally {
          fos.close
        }
        
        e = tis.getNextTarEntry
      }
    } finally {
      tis.close
    }

  }
}
