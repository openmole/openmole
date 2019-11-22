/*
 * Copyright (C) 02/10/13 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.serializer.file

import org.openmole.tool.file._
import org.openmole.tool.tar._
import org.openmole.core.workspace.{ TmpDirectory, Workspace }
import org.openmole.tool.tar.TarOutputStream

import scala.collection.immutable.HashMap
import java.util.UUID
import java.io.{ File, FileOutputStream }

import com.thoughtworks.xstream.XStream
import org.openmole.core.serializer.converter.Serialiser

object FileSerialisation {
  case class FileInfo(originalPath: String, directory: Boolean, exists: Boolean)
  type FilesInfo = HashMap[String, FileInfo]

  def filesInfo = "filesInfo.xml"
  def fileDir = "files"

  def serialiseFiles(files: Iterable[File], tos: TarOutputStream, xStream: XStream)(implicit newFile: TmpDirectory) = newFile.withTmpDir { tmpDir ⇒
    val fileInfo = HashMap() ++ files.map {
      file ⇒
        val name = UUID.randomUUID

        val toArchive =
          if (file.isDirectory) {
            val toArchive = tmpDir.newFile("archive", ".tar")
            file.archive(toArchive)
            toArchive
          }
          else file

        if (toArchive.exists)
          tos.addFile(toArchive, fileDir + "/" + name.toString)

        (name.toString, FileInfo(file.getPath, file.isDirectory, file.exists))
    }

    newFile.withTmpFile { tmpFile ⇒
      tmpFile.withOutputStream(xStream.toXML(fileInfo, _))
      tos.addFile(tmpFile, fileDir + "/" + filesInfo)
    }
  }

  def deserialiseFileReplacements(archiveExtractDir: File, xStream: XStream)(implicit newFile: TmpDirectory) = {
    val fileInfoFile = archiveExtractDir / s"$fileDir/$filesInfo"
    val fi = fileInfoFile.withInputStream(xStream.fromXML).asInstanceOf[FilesInfo]

    HashMap() ++ fi.map {
      case (name, FileInfo(originalPath, isDirectory, exists)) ⇒
        val fromArchive = new File(archiveExtractDir, fileDir + "/" + name)

        def fileContent =
          if (isDirectory) {
            val dest = newFile.newDir("directoryFromArchive")
            dest.mkdirs()
            if (exists) fromArchive.extract(dest)
            else dest.delete
            dest
          }
          else {
            val dest = newFile.newFile("fileFromArchive", ".bin")
            dest.createParentDir
            if (exists) fromArchive.move(dest)
            else dest.delete
            dest
          }

        originalPath → fileContent
    }

  }

}
