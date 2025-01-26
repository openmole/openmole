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
import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.core.{Caching, ClassLoaderReference, DefaultConverterLookup}
import com.thoughtworks.xstream.converters.{Converter, ConverterRegistry}
import com.thoughtworks.xstream.io.json.*
import com.thoughtworks.xstream.mapper.Mapper
import com.thoughtworks.xstream.security.*
import com.thoughtworks.xstream.io.binary.BinaryStreamDriver
import org.openmole.tool.file.*
import org.openmole.core.serializer.converter.*

import java.util.concurrent.locks.{ReadWriteLock, ReentrantReadWriteLock}
import org.openmole.core.fileservice.FileService
import org.openmole.core.workspace.{TmpDirectory, Workspace}
import org.openmole.tool.logger.JavaLogger
import org.openmole.tool.stream
import org.openmole.tool.archive.*
import org.openmole.tool.lock.*

import collection.mutable.ListBuffer
import org.openmole.core.serializer.file.{FileInjection, FileSerialisation, FileWithGCConverter}

import scala.util.NotGiven
import org.apache.fury.*
import org.apache.fury.config.*
import org.apache.fury.io.*
import org.apache.fury.memory.*
import org.apache.fury.serializer.*
import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.fileservice.FileService.FileWithGC
import org.openmole.core.workspace

object SerializerService:
  org.apache.fury.logging.LoggerFactory.disableLogging()
  def apply() = new SerializerService
  def stub() = apply()

/**
 * Serializer
 */
class SerializerService:
  service =>

  def buildXStream() =
    val lookup = new DefaultConverterLookup()
    val driver = new BinaryStreamDriver()

    val xs =
      new XStream(
        null,
        driver,
        new ClassLoaderReference(service.getClass.getClassLoader),
        null: Mapper,
        lookup,
        new ConverterRegistry:
          override def registerConverter(c: Converter, p: Int): Unit = lookup.registerConverter(c, p)
      )

    xs.addPermission(NoTypePermission.NONE)
    xs.addPermission(new TypePermission {
      override def allows(`type`: Class[?]): Boolean = true
    })
    //xs.registerConverter(new converter.fix.HashMapConverter(xs.getMapper))

    xs.registerConverter(new FileWithGCConverter)

    xs

  private val content = "content.xml"

  private def fileListing() = new FilesListing(buildXStream())
  private def pluginAndFileListing() = new PluginAndFilesListing(buildXStream())
  private def deserializerWithFileInjection() = new FileInjection(buildXStream())

  //  def deserialize[T](file: File): T = file.withFileInputStream(deserialize[T])

  //  def deserialize[T](is: InputStream): T =
//    buildXStream().fromXML(is).asInstanceOf[T]

//  def deserializeAndExtractFiles[T](file: File, deleteFilesOnGC: Boolean, gz: Boolean = false)(implicit newFile: TmpDirectory, fileService: FileService): T =
//    val tis = TarArchiveInputStream(file.bufferedInputStream(gz = gz))
//    try deserializeAndExtractFiles(tis, deleteFilesOnGC = deleteFilesOnGC)
//    finally tis.close

//  def deserializeAndExtractFiles[T](tis: TarArchiveInputStream, deleteFilesOnGC: Boolean)(implicit newFile: TmpDirectory, fileService: FileService): T =
//    TmpDirectory.withTmpDir: archiveExtractDir ⇒
//      tis.extract(archiveExtractDir)
//      val fileReplacement = FileSerialisation.deserialiseFileReplacements(archiveExtractDir, fileSerialisation(), deleteOnGC = deleteFilesOnGC)
//      val contentFile = new File(archiveExtractDir, content)
//      deserializeReplaceFiles[T](contentFile, fileReplacement, gz = false)


//  def serializeAndArchiveFiles(obj: Any, f: File, gz: Boolean = false)(implicit newFile: TmpDirectory): Unit =
//    val os = TarArchiveOutputStream(f.bufferedOutputStream(gz = gz))
//    try serializeAndArchiveFiles(obj, os)
//    finally os.close()

//  def serializeAndArchiveFiles(obj: Any, tos: TarArchiveOutputStream)(implicit newFile: TmpDirectory): Unit =
//    TmpDirectory.withTmpFile: objSerial ⇒
//      serialize(obj, objSerial)
//      tos.addFile(objSerial, content)
//
//    val files = fileListing().list(obj)
//    FileSerialisation.serialiseFiles(files, tos, fileSerialisation())

  def listFiles(obj: Any) = fileListing().list(obj)
  def listPluginsAndFiles(obj: Any) = pluginAndFileListing().list(obj)


  def buildFury() =
    import org.apache.fury.serializer.scala.ScalaSerializers

    class FileWithGCGerializer(fury: Fury) extends Serializer(fury, classOf[File]):
      lazy val fileSerializer = fury.getClassResolver.getSerializer(classOf[File])
      override def write(buffer: MemoryBuffer, value: File): Unit = fileSerializer.write(buffer, new File(value.asInstanceOf[FileWithGC].getPath))
      override def read(buffer: MemoryBuffer): File = fileSerializer.read(buffer)

    val fury =
      Fury.builder().withLanguage(Language.JAVA)
        //.withScalaOptimizationEnabled(true)
        .requireClassRegistration(false)
        .withClassLoader(SerializerService.getClass.getClassLoader)
        .withRefTracking(true)
        .suppressClassRegistrationWarnings(true)
        .build()

    //ScalaSerializers.registerSerializers(fury)
    fury.registerSerializer(classOf[FileWithGC], new FileWithGCGerializer(fury))

    fury

  //lazy val furyInstance = buildFury()


  def deserializeReplaceFiles[T](is: InputStream, files: Map[String, File]): T =
    def inject[T](fury: Fury, is: InputStream, files: Map[String, File]) =
      class FileInjector(fury: Fury, fileSerializer: Serializer[File], inject: Map[String, File]) extends Serializer(fury, classOf[File]):
        override def write(buffer: MemoryBuffer, value: File): Unit = ???
        override def read(buffer: MemoryBuffer): File =
          val file = fileSerializer.read(buffer)
          inject.getOrElse(file.getPath, throw InternalProcessingError(s"Replacement for file $file not found among $inject"))

      val fileSerializer = fury.getClassResolver.getSerializer(classOf[File])
      fury.registerSerializer(classOf[File], new FileInjector(fury, fileSerializer, files))
      fury.deserialize(new FuryInputStream(is)).asInstanceOf[T]

    inject[T](buildFury(), is, files)

  def deserialize[T](is: InputStream)(using NotGiven[T =:= Nothing]): T = buildFury().deserialize(new FuryInputStream(is)).asInstanceOf[T]

  def deserialize[T](file: File)(using NotGiven[T =:= Nothing]): T =
    val is = new FileInputStream(file)
    try deserialize[T](is)
    finally is.close

  def deserializeReplaceFiles[T](file: File, files: Map[String, File], gz: Boolean = false): T =
    val is = file.bufferedInputStream(gz = gz)
    try deserializeReplaceFiles[T](is, files)
    finally is.close()

  def deserializeAndExtractFiles[T](file: File, deleteFilesOnGC: Boolean, gz: Boolean = false)(using TmpDirectory, FileService): T =
    val tis = TarArchiveInputStream(file.bufferedInputStream(gz = gz))
    try deserializeAndExtractFiles(tis, deleteFilesOnGC = deleteFilesOnGC)
    finally tis.close

  def deserializeAndExtractFiles[T](tis: TarArchiveInputStream, deleteFilesOnGC: Boolean)(using TmpDirectory, FileService): T =
    summon[TmpDirectory].withTmpDir: archiveExtractDir ⇒
      tis.extract(archiveExtractDir)
      val fileReplacement = FileSerialisation.deserialiseFileReplacements(archiveExtractDir, buildFury(), deleteOnGC = deleteFilesOnGC)
      val contentFile = new File(archiveExtractDir, content)
      deserializeReplaceFiles[T](contentFile, fileReplacement, gz = false)

  def serialize(obj: Any, os: OutputStream) = buildFury().serialize(os, obj)

  def serialize(obj: Any, file: File, gz: Boolean = false): Unit =
    val os = file.bufferedOutputStream(gz = gz)
    try buildFury().serialize(os, obj)
    finally os.close()

  def serializeAndArchiveFiles(obj: Any, f: File, gz: Boolean = false)(using TmpDirectory): Unit =
    val os = TarArchiveOutputStream(f.bufferedOutputStream(gz = gz))
    try serializeAndArchiveFiles(obj, os)
    finally os.close()

  def serializeAndArchiveFiles(obj: Any, tos: TarArchiveOutputStream)(using TmpDirectory): Unit =
    summon[TmpDirectory].withTmpFile: objSerial ⇒
      serialize(obj, objSerial)
      tos.addFile(objSerial, content)

    val files = listFiles(obj)
    FileSerialisation.serialiseFiles(files, tos, buildFury())
