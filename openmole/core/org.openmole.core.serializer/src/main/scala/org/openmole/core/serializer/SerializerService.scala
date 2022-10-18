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

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import com.thoughtworks.xstream.core.{ Caching, ClassLoaderReference, DefaultConverterLookup }
import com.thoughtworks.xstream.io.binary.BinaryStreamDriver
import org.openmole.tool.file._
import org.openmole.core.serializer.converter._

import java.util.concurrent.locks.{ ReadWriteLock, ReentrantReadWriteLock }
import com.thoughtworks.xstream.converters.{ Converter, ConverterRegistry }
import com.thoughtworks.xstream.io.json._
import com.thoughtworks.xstream.mapper.Mapper
import com.thoughtworks.xstream.security._
import org.openmole.core.fileservice.FileService
import org.openmole.core.workspace.{ TmpDirectory, Workspace }
import org.openmole.tool.logger.JavaLogger
import org.openmole.tool.stream
import org.openmole.tool.tar._
import org.openmole.tool.lock._

import collection.mutable.ListBuffer
import org.openmole.core.serializer.file.{ FileInjection, FileSerialisation, FileWithGCConverter }

object SerializerService {
  def apply() = new SerializerService
}

/**
 * Serializer
 */
class SerializerService { service ⇒

  private[serializer] def buildXStream(json: Boolean = false) = {
    val lookup = new DefaultConverterLookup()

    val driver =
      if (json) new JsonHierarchicalStreamDriver() else new BinaryStreamDriver()

    val xs =
      new XStream(
        null,
        driver,
        new ClassLoaderReference(this.getClass.getClassLoader),
        null: Mapper,
        lookup,
        new ConverterRegistry {
          override def registerConverter(c: Converter, p: Int): Unit = lookup.registerConverter(c, p)
        }
      )

    xs.addPermission(NoTypePermission.NONE)
    xs.addPermission(new TypePermission {
      override def allows(`type`: Class[_]): Boolean = true
    })
    //xs.registerConverter(new converter.fix.HashMapConverter(xs.getMapper))

    xs.registerConverter(new FileWithGCConverter)

    xs
  }

  private val content = "content.xml"

  private def fileSerialisation() = buildXStream()
  private def pluginAndFileListing() = new Serialiser(service) with PluginAndFilesListing
  private def deserializerWithFileInjection() = new Serialiser(service) with FileInjection

  def deserialize[T](file: File): T = {
    val is = new FileInputStream(file)
    try deserialize(is)
    finally is.close
  }

  def deserialize[T](is: InputStream): T = buildXStream().fromXML(is).asInstanceOf[T]

  def deserializeFromString[T](s: String, json: Boolean = false): T = buildXStream(json = json).fromXML(s).asInstanceOf[T]

  def deserializeAndExtractFiles[T](file: File, deleteFilesOnGC: Boolean, gz: Boolean = false)(implicit newFile: TmpDirectory, fileService: FileService): T = {
    val tis = new TarInputStream(file.bufferedInputStream(gz = gz))
    try deserializeAndExtractFiles(tis, deleteFilesOnGC = deleteFilesOnGC)
    finally tis.close
  }

  def deserializeAndExtractFiles[T](tis: TarInputStream, deleteFilesOnGC: Boolean)(implicit newFile: TmpDirectory, fileService: FileService): T = {
    newFile.withTmpDir { archiveExtractDir ⇒
      tis.extract(archiveExtractDir)
      val fileReplacement = FileSerialisation.deserialiseFileReplacements(archiveExtractDir, fileSerialisation(), deleteOnGC = deleteFilesOnGC)
      val contentFile = new File(archiveExtractDir, content)
      deserializeReplaceFiles[T](contentFile, fileReplacement, gz = false)
    }
  }

  def serializeAndArchiveFiles(obj: Any, f: File, gz: Boolean = false)(implicit newFile: TmpDirectory): Unit = {
    val os = new TarOutputStream(f.bufferedOutputStream(gz = gz))
    try serializeAndArchiveFiles(obj, os)
    finally os.close
  }

  def serializeAndArchiveFiles(obj: Any, tos: TarOutputStream)(implicit newFile: TmpDirectory): Unit = {
    newFile.withTmpFile { objSerial ⇒
      serialize(obj, objSerial)
      tos.addFile(objSerial, content)
    }
    val serializationResult = pluginsAndFiles(obj)
    FileSerialisation.serialiseFiles(serializationResult.files, tos, fileSerialisation())
  }

  def pluginsAndFiles(obj: Any) = pluginAndFileListing().list(obj)

  def deserializeReplaceFiles[T](file: File, files: Map[String, File], gz: Boolean = false): T = {
    val is = file.bufferedInputStream(gz = gz)
    try deserializeReplaceFiles[T](is, files)
    finally is.close
  }

  def deserializeReplaceFiles[T](is: InputStream, files: Map[String, File]): T = {
    val serializer = deserializerWithFileInjection()
    serializer.injectedFiles = files
    serializer.fromXML[T](is)
  }

  def serializeToString(obj: Any, json: Boolean = false) = buildXStream(json = json).toXML(obj)

  def serialize(obj: Any, os: OutputStream) = buildXStream().toXML(obj, os)

  def serialize(obj: Any, file: File, json: Boolean = false, gz: Boolean = false): Unit = {
    val os = file.bufferedOutputStream(gz = gz)
    try buildXStream(json = json).toXML(obj, os)
    finally os.close
  }

}
