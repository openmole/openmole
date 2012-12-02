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

package org.openmole.core.serializer

import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.io.xml.StaxDriver
import java.io.File
import java.io.FileFilter
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.tools.io.TarArchiver._
import org.openmole.core.serializer.structure.PluginClassAndFiles
import java.util.UUID
import java.util.concurrent.Semaphore
import org.openmole.core.serializer.converter._
import org.openmole.core.serializer.structure.FileInfo
import com.ice.tar.TarEntry
import com.ice.tar.TarOutputStream
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.workspace.Workspace
import collection.JavaConversions._
import scala.collection.immutable.TreeMap
import com.ice.tar.TarInputStream

object SerializerService extends Logger {

  private val xstream = new XStream
  private val filesInfo = "filesInfo.xml"
  private val content = "content.xml"

  private type FilesInfo = TreeMap[String, (File, Boolean, Boolean)]

  def deserialize[T](file: File): T = {
    val is = new FileInputStream(file)
    try deserialize(is)
    finally is.close
  }

  def deserialize[T](is: InputStream): T = xstream.fromXML(is).asInstanceOf[T]

  def deserializeAndExtractFiles[T](file: File) = {
    val extractDir = Workspace.newDir("extraction")
    file.extractDirArchiveWithRelativePath(extractDir)

    val fileInfoFile = new File(extractDir, filesInfo)
    val fi = deserialize[FilesInfo](fileInfoFile)
    fileInfoFile.delete

    val fileReplacement =
      new TreeMap[File, File] ++ fi.map {
        case (name, (file, isDirectory, exists)) ⇒
          val f = new File(extractDir, name)
          val dest = Workspace.newFile("extracted", ".bin")
          if (exists) f.move(dest)

          file ->
            (if (isDirectory) {
              val extractedDir = Workspace.newDir("extractionDir")
              dest.extractDirArchiveWithRelativePath(extractedDir)
              dest.delete

              extractedDir
            } else dest)
      }

    val contentFile = new File(extractDir, content)
    val obj = deserializeReplaceFiles[T](contentFile, fileReplacement)
    contentFile.delete
    extractDir.delete

    obj
  }

  def serializeAndArchiveFiles(obj: Any, file: File) = {
    val objSerial = Workspace.newFile
    val serializationResult = serializeGetPluginsAndFiles(obj, objSerial)

    val tos = new TarOutputStream(new FileOutputStream(file))

    try {
      tos.addFile(objSerial, content)
      objSerial.delete

      val fileInfo = new FilesInfo ++ serializationResult.files.map {
        file ⇒
          val name = UUID.randomUUID

          val toArchive =
            if (file.isDirectory) {
              val toArchive = Workspace.newFile
              val outputStream = new TarOutputStream(new FileOutputStream(toArchive))

              try outputStream.createDirArchiveWithRelativePath(file)
              finally outputStream.close

              toArchive
            } else file

          if (toArchive.exists)
            tos.addFile(toArchive, name.toString)

          (name.toString, (file, file.isDirectory, file.exists))
      }

      val filesInfoSerial = Workspace.newFile
      serialize(fileInfo, filesInfoSerial)
      tos.addFile(filesInfoSerial, filesInfo)
      filesInfoSerial.delete
    } finally tos.close
  }

  def serializeFilePathAsHashGetFiles(obj: Any, file: File): Map[File, FileInfo] = {
    val os = file.bufferedOutputStream
    try serializeFilePathAsHashGetFiles(obj, os)
    finally os.close
  }

  def serializeFilePathAsHashGetFiles(obj: Any, os: OutputStream): Map[File, FileInfo] = {
    val serializer = new SerializerWithPathHashInjection
    serializer.toXML(obj.asInstanceOf[AnyRef], os)
  }

  def serializeGetPluginsAndFiles(obj: Any, file: File): PluginClassAndFiles = {
    val os = file.bufferedOutputStream
    try serializeGetPluginsAndFiles(obj, os)
    finally os.close
  }

  def serializeGetPluginsAndFiles(obj: Any, os: OutputStream): PluginClassAndFiles =
    SerializerWithFileAndPluginListingFactory.exec {
      serializer ⇒
        val (files, plugins) = serializer.toXMLAndListPluginFiles(obj.asInstanceOf[AnyRef], os)
        new PluginClassAndFiles(files, plugins)
    }

  def deserializeReplaceFiles[T](file: File, files: PartialFunction[File, File]): T = {
    val is = file.bufferedInputStream
    try deserializeReplaceFiles[T](is, files)
    finally is.close
  }

  def deserializeReplaceFiles[T](is: InputStream, files: PartialFunction[File, File]): T =
    DeserializerWithFileInjectionFromFileFactory.exec {
      serializer ⇒
        serializer.files = files
        serializer.fromXML[T](is)
    }

  def serialize(obj: Any, os: OutputStream) = xstream.toXML(obj, os)

  def serialize(obj: Any, file: File): Unit = {
    val os = file.bufferedOutputStream
    try serialize(obj, os)
    finally os.close
  }

  def deserializeReplacePathHash[T](file: File, files: PartialFunction[FileInfo, File]): T = {
    val is = file.bufferedInputStream
    try deserializeReplacePathHash[T](is, files)
    finally is.close
  }

  def deserializeReplacePathHash[T](is: InputStream, files: PartialFunction[FileInfo, File]) =
    DeserializerWithFileInjectionFromPathHashFactory.exec {
      deserializer ⇒
        deserializer.files = files
        deserializer.fromXML[T](is)
    }
}
