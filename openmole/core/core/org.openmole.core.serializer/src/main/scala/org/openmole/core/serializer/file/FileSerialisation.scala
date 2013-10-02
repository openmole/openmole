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

import scala.collection.immutable.TreeMap
import java.util.UUID
import org.openmole.misc.workspace.Workspace
import com.ice.tar.TarOutputStream
import java.io.{ File, FileOutputStream }
import org.openmole.misc.tools.io.TarArchiver._
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.core.serializer.SerializerService

trait FileSerialisation {

  type FilesInfo = TreeMap[String, (File, Boolean, Boolean)]

  val filesInfo = "filesInfo.xml"

  def serialiseFiles(files: Iterable[File], tos: TarOutputStream) = {
    val fileInfo = new FilesInfo ++ files.map {
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
          tos.addFile(toArchive, name.toString)

        (name.toString, (file, file.isDirectory, file.exists))
    }

    val filesInfoSerial = Workspace.newFile
    SerializerService.serialize(fileInfo, filesInfoSerial)
    tos.addFile(filesInfoSerial, filesInfo)
    filesInfoSerial.delete
  }

  def deserialiseFileReplacements(archiveExtractDir: File, extractDir: File) = {
    val fileInfoFile = new File(archiveExtractDir, filesInfo)
    val fi = SerializerService.deserialize[FilesInfo](fileInfoFile)
    fileInfoFile.delete

    new TreeMap[File, File] ++ fi.map {
      case (name, (file, isDirectory, exists)) ⇒
        val f = new File(archiveExtractDir, name)
        val dest = extractDir.newFile("extracted", ".bin")
        if (exists) f.move(dest)

        file ->
          (if (isDirectory) {
            val extractedDir = extractDir.newDir("extractionDir")
            dest.extractDirArchiveWithRelativePath(extractedDir)
            dest.delete

            extractedDir
          }
          else dest)
    }

  }

}
