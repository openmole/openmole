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

import scala.collection.immutable.HashMap
import java.util.UUID
import org.openmole.misc.workspace.Workspace
import com.ice.tar.TarOutputStream
import java.io.{ File, FileOutputStream }
import org.openmole.misc.tools.io.TarArchiver._
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.core.serializer.converter.Serialiser

object FileSerialisation {
  case class FileInfo(file: File, directory: Boolean, exists: Boolean)
  type FilesInfo = HashMap[String, FileInfo]
}

import FileSerialisation._

trait FileSerialisation extends Serialiser {

  def filesInfo = "filesInfo.xml"
  def fileDir = "files"

  def serialiseFiles(files: Iterable[File], tos: TarOutputStream) = {
    val fileInfo = HashMap() ++ files.map {
      file ⇒
        val name = UUID.randomUUID

        val toArchive =
          if (file.isDirectory) {
            val toArchive = Workspace.newFile
            val outputStream = new TarOutputStream(new FileOutputStream(toArchive))

            try outputStream.createDirArchiveWithRelativePath(file)
            finally outputStream.close

            toArchive
          }
          else file

        if (toArchive.exists)
          tos.addFile(toArchive, fileDir + "/" + name.toString)

        (name.toString, FileInfo(file, file.isDirectory, file.exists))
    }

    Workspace.withTmpFile { tmpFile ⇒
      tmpFile.withOutputStream(xStream.toXML(fileInfo, _))
      tos.addFile(tmpFile, fileDir + "/" + filesInfo)
    }
  }

  def deserialiseFileReplacements(archiveExtractDir: File, extractDir: File, shortNames: Boolean = true) = {
    val fileInfoFile = new File(archiveExtractDir, fileDir + "/" + filesInfo)
    val fi = fileInfoFile.withInputStream(xStream.fromXML).asInstanceOf[FilesInfo]
    fileInfoFile.delete

    HashMap() ++ fi.map {
      case (name, FileInfo(file, isDirectory, exists)) ⇒
        val fromArchive = new File(archiveExtractDir, fileDir + "/" + name)

        def fileContent =
          if (isDirectory) {
            val dest = if (shortNames) extractDir.newDir("extracted")
            else {
              val f = new File(extractDir, file.getAbsolutePath)
              f.mkdirs()
              f
            }
            if (exists) fromArchive.extractDirArchiveWithRelativePath(dest)
            else dest.delete
            dest
          }
          else {
            val dest =
              if (shortNames) extractDir.newFile("extracted", ".bin")
              else {
                val f = new File(extractDir, file.getAbsolutePath)
                f.getParentFile.mkdirs()
                f
              }
            if (exists) fromArchive.move(dest)
            else dest.delete

            dest
          }

        file -> fileContent
    }

  }

}
