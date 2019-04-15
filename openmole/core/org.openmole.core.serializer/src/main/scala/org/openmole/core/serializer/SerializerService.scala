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

import com.thoughtworks.xstream.converters.Converter
import com.thoughtworks.xstream.mapper.Mapper
import com.thoughtworks.xstream.security._
import org.openmole.core.workspace.{ NewFile, Workspace }
import org.openmole.tool.logger.JavaLogger
import org.openmole.tool.stream
import org.openmole.tool.tar._
import org.openmole.tool.lock._

import collection.mutable.ListBuffer
import org.openmole.core.serializer.file.{ FileInjection, FileSerialisation }

object SerializerService {
  def apply() = new SerializerService
}

/**
 * Serializer
 */
class SerializerService { service ⇒

  private[serializer] def buildXStream() = {
    val lookup = new DefaultConverterLookup()

    val xs =
      new XStream(
        null,
        new BinaryStreamDriver(),
        new ClassLoaderReference(this.getClass.getClassLoader),
        null: Mapper,
        lookup,
        (c: Converter, p: Int) ⇒ lookup.registerConverter(c, p))

    xs.addPermission(NoTypePermission.NONE)
    xs.addPermission(new TypePermission {
      override def allows(`type`: Class[_]): Boolean = true
    })
    xs
  }

  private val content = "content.xml"

  private def fileSerialisation() = buildXStream
  private def pluginAndFileListing() = new Serialiser(service) with PluginAndFilesListing
  private def deserializerWithFileInjection() = new Serialiser(service) with FileInjection

  def deserialize[T](file: File): T = {
    val is = new FileInputStream(file)
    try deserialize(is)
    finally is.close
  }

  def deserialize[T](is: InputStream): T = buildXStream().fromXML(is).asInstanceOf[T]

  def deserializeAndExtractFiles[T](file: File)(implicit newFile: NewFile): (T, Iterable[File]) = {
    val tis = new TarInputStream(file.bufferedInputStream)
    try deserializeAndExtractFiles(tis)
    finally tis.close
  }

  def deserializeAndExtractFiles[T](tis: TarInputStream)(implicit newFile: NewFile): (T, Iterable[File]) = {
    newFile.withTmpDir { archiveExtractDir ⇒
      tis.extract(archiveExtractDir)
      val fileReplacement = FileSerialisation.deserialiseFileReplacements(archiveExtractDir, fileSerialisation())
      val contentFile = new File(archiveExtractDir, content)
      (deserializeReplaceFiles[T](contentFile, fileReplacement), fileReplacement.values)
    }
  }

  def serializeAndArchiveFiles(obj: Any, f: File)(implicit newFile: NewFile): Unit = {
    val os = new TarOutputStream(f.bufferedOutputStream())
    try serializeAndArchiveFiles(obj, os)
    finally os.close
  }

  def serializeAndArchiveFiles(obj: Any, tos: TarOutputStream)(implicit newFile: NewFile): Unit = {
    newFile.withTmpFile { objSerial ⇒
      serialize(obj, objSerial)
      tos.addFile(objSerial, content)
    }
    val serializationResult = pluginsAndFiles(obj)
    FileSerialisation.serialiseFiles(serializationResult.files, tos, fileSerialisation())
  }

  def pluginsAndFiles(obj: Any) = pluginAndFileListing().list(obj)

  def deserializeReplaceFiles[T](file: File, files: PartialFunction[String, File]): T = {
    val is = file.bufferedInputStream
    try deserializeReplaceFiles[T](is, files)
    finally is.close
  }

  def deserializeReplaceFiles[T](is: InputStream, files: PartialFunction[String, File]): T = {
    val serializer = deserializerWithFileInjection()
    serializer.injectedFiles = files
    serializer.fromXML[T](is)
  }

  def serialize(obj: Any) = buildXStream().toXML(obj)

  def serialize(obj: Any, os: OutputStream) = buildXStream().toXML(obj, os)

  def serialize(obj: Any, file: File): Unit = {
    val os = file.bufferedOutputStream()
    try serialize(obj, os)
    finally os.close
  }

}
