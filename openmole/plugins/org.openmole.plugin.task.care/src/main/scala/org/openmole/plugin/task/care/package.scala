/**
 * Copyright (C) 2015 Jonathan Passerat-Palmbach
 * Copyright (C) 2015 Mathieu Leclaire
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

package org.openmole.plugin.task

import java.nio.channels.FileChannel
import java.util.zip.GZIPInputStream
import java.io._
import org.openmole.tool.file._
import org.openmole.tool.stream.StringOutputStream
import org.openmole.tool.tar._

package care {
  trait CAREPackage //extends systemexec.SystemExecPackage
}

package object care extends care.CAREPackage {

  import resource._

  def managedArchive(careArchive: File) = managed(new RandomAccessFile(careArchive, "r")) map (_.getChannel)

  def extractArchive(archive: ManagedResource[FileChannel]) = archive.map { fileChannel ⇒

    //Get the tar.gz from the bin archive
    val endMinus8Bytes = fileChannel.size - 8L
    val archiveSize = fileChannel.map(FileChannel.MapMode.READ_ONLY, endMinus8Bytes, 8L).getLong.toInt
    fileChannel.position(0L)
    val srcArray = new Array[Byte](archiveSize)
    fileChannel.map(FileChannel.MapMode.READ_ONLY, endMinus8Bytes - 13L - archiveSize, archiveSize).get(srcArray, 0, archiveSize)

    //Extract and uncompress the tar.gz
    val stream = managed(new TarInputStream(new GZIPInputStream(new ByteArrayInputStream(srcArray))))
    stream
  }.opt.get

  case class CAREInfo(commandLine: Option[Seq[String]], workDirectory: Option[String])

  def getCareBinInfos(careArchive: File): CAREInfo = getCareBinInfos(extractArchive(managedArchive(careArchive)))

  /** The .opt.get at the end will force all operations to happen and close the managed resources */
  def getCareBinInfos(extractedArchiveStream: ManagedResource[TarInputStream]) =
    extractedArchiveStream.map { stream ⇒

      Iterator.continually(stream.getNextEntry).dropWhile { te ⇒
        val pathString = te.getName.split("/")
        pathString.last != "re-execute.sh" || pathString.contains("rootfs")
      }.toSeq.headOption.flatMap { _ ⇒

        val linesManaged = managed(new StringOutputStream) map { stringW: StringOutputStream ⇒
          stream copy stringW
          stringW.toString.split("\n")
        }
        val lines = linesManaged.opt.get

        val prootLine = lines.indexWhere(s ⇒ s.startsWith("PROOT="))
        val commands =
          if (prootLine != -1) {
            // get only the command lines, and strip each component from its single quotes and final backslash
            Some(lines.slice(7, prootLine - 1).map { l ⇒ l.dropRight(2) }.map { _.drop(1) }.map { _.dropRight(1) }.toSeq)
          }
          else None

        val workdirLine = lines.find(_.startsWith("-w '"))
        val workDirectory = workdirLine.flatMap { line ⇒
          // FIXME split can fail
          line.split("'").find(_.startsWith("/"))
        }

        Some(CAREInfo(commands, workDirectory))
        // FIXME anything less ugly maybe?
      }.get
    }.opt.get
}
