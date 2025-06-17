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
import org.openmole.tool.cache.*
import collection.mutable.ListBuffer
import org.openmole.core.serializer.file.{FileInjection, FileSerialisation, FileWithGCConverter}
import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.fileservice.FileService.FileWithGC
import org.openmole.core.workspace

import scala.util.NotGiven

object SerializerService:
  private[serializer] val content = "content.xml"

  def apply(): SerializerService = SerializerService(new XStreamSerializerService)
  def stub(): SerializerService = apply()

  def buildXStream() =
    val lookup = new DefaultConverterLookup()
    val driver = new BinaryStreamDriver()

    val xs =
      new XStream(
        null,
        driver,
        new ClassLoaderReference(SerializerService.getClass.getClassLoader),
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

case class SerializerService(service: SerializerServiceInterface):
  export service.*

trait SerializerServiceInterface:
  private[serializer] val fileListing = WithInstance()(() => new FilesListing(SerializerService.buildXStream()))
  private[serializer] val pluginAndFileListing = WithInstance()(() => new PluginAndFilesListing(SerializerService.buildXStream()))
  private[serializer] val xStream = WithInstance()(() => SerializerService.buildXStream())

  def listFiles(obj: Any) = fileListing(_.list(obj))
  def listPluginsAndFiles(obj: Any) = pluginAndFileListing(_.list(obj))

  def deserialize[T](file: File)(using NotGiven[T =:= Nothing]): T = file.withFileInputStream(deserialize[T])
  def deserialize[T](is: InputStream)(using NotGiven[T =:= Nothing]): T
  def deserializeAndExtractFiles[T](file: File, deleteFilesOnGC: Boolean, gz: Boolean = false)(using TmpDirectory, FileService): T =
    val tis = TarArchiveInputStream(file.bufferedInputStream(gz = gz))
    try deserializeAndExtractFiles(tis, deleteFilesOnGC = deleteFilesOnGC)
    finally tis.close

  def deserializeAndExtractFiles[T](tis: TarArchiveInputStream, deleteFilesOnGC: Boolean)(using TmpDirectory, FileService): T

  def deserializeReplaceFiles[T](file: File, files: Map[String, File], gz: Boolean = false): T =
    val is = file.bufferedInputStream(gz = gz)
    try deserializeReplaceFiles[T](is, files)
    finally is.close()

  def deserializeReplaceFiles[T](is: InputStream, files: Map[String, File]): T

  def serializeAndArchiveFiles(obj: Any, f: File, gz: Boolean = false)(using TmpDirectory): Unit =
    val os = TarArchiveOutputStream(f.bufferedOutputStream(gz = gz))
    try serializeAndArchiveFiles(obj, os)
    finally os.close()

  def serializeAndArchiveFiles(obj: Any, tos: TarArchiveOutputStream)(using TmpDirectory): Unit

  def serialize(obj: Any, os: OutputStream): Unit

  def serialize(obj: Any, file: File, gz: Boolean = false): Unit


/**
 * Serializer
 */
class XStreamSerializerService extends SerializerServiceInterface:
  service =>

  val deserializerWithFileInjection = WithInstance()(() => new FileInjection(SerializerService.buildXStream()))

  def deserialize[T](is: InputStream)(using NotGiven[T =:= Nothing]): T = xStream(_.fromXML(is).asInstanceOf[T])

  def deserializeAndExtractFiles[T](tis: TarArchiveInputStream, deleteFilesOnGC: Boolean)(using TmpDirectory, FileService): T =
    TmpDirectory.withTmpDir: archiveExtractDir =>
      tis.extract(archiveExtractDir)
      xStream: xs =>
        val fileReplacement = FileSerialisation.deserialiseFileReplacements(archiveExtractDir, xs.fromXML, deleteOnGC = deleteFilesOnGC)
        val contentFile = new File(archiveExtractDir, SerializerService.content)
        deserializeReplaceFiles[T](contentFile, fileReplacement, gz = false)

  def deserializeReplaceFiles[T](is: InputStream, files: Map[String, File]): T =
    deserializerWithFileInjection(_.fromXML[T](is, files))

  def serializeAndArchiveFiles(obj: Any, tos: TarArchiveOutputStream)(using TmpDirectory): Unit =
    TmpDirectory.withTmpFile: objSerial =>
      serialize(obj, objSerial)
      tos.addFile(objSerial, SerializerService.content)

    val files = fileListing(_.list(obj))
    xStream: xs =>
      FileSerialisation.serialiseFiles(files, tos, (os, obj) => xs.toXML(obj, os))

  def serialize(obj: Any, os: OutputStream) = xStream(_.toXML(obj, os))

  def serialize(obj: Any, file: File, gz: Boolean = false): Unit =
    val os = file.bufferedOutputStream(gz = gz)
    try xStream(_.toXML(obj, os))
    finally os.close()


  /* ------------  fory ------------ */

//class ForySerializer:
//
//  import scala.util.NotGiven
//  import org.apache.fory.*
//  import org.apache.fory.config.*
//  import org.apache.fory.io.*
//  import org.apache.fory.memory.*
//  import org.apache.fory.serializer.*
//
//  def buildFory() =
//    org.apache.fory.logging.LoggerFactory.disableLogging()
//
//    import org.apache.fory.serializer.scala.ScalaSerializers
//
//    class FileWithGCGerializer(fory: Fory) extends Serializer(fory, classOf[File]):
//      lazy val fileSerializer = fory.getClassResolver.getSerializer(classOf[File])
//      override def write(buffer: MemoryBuffer, value: File): Unit = fileSerializer.write(buffer, new File(value.asInstanceOf[FileWithGC].getPath))
//      override def read(buffer: MemoryBuffer): File = fileSerializer.read(buffer)
//
//    val fory =
//      Fory.builder().withLanguage(Language.JAVA)
//        .withScalaOptimizationEnabled(true)
//        .requireClassRegistration(false)
//        .withClassLoader(SerializerService.getClass.getClassLoader)
//        .withRefTracking(true)
//        .suppressClassRegistrationWarnings(true)
//        .build()
//
//    ScalaSerializers.registerSerializers(fory)
//    fory.registerSerializer(classOf[FileWithGC], new FileWithGCGerializer(fory))
//
//    fory
//
//  val serializer = WithInstance()(buildFory)
//
//  //  def deserializeReplaceFiles[T](is: InputStream, files: Map[String, File]): T =
////    def inject[T](fory: Fory, is: InputStream, files: Map[String, File]) =
////      class FileInjector(fory: Fory, fileSerializer: Serializer[File], inject: Map[String, File]) extends Serializer(fory, classOf[File]):
////        override def write(buffer: MemoryBuffer, value: File): Unit = ???
////        override def read(buffer: MemoryBuffer): File =
////          val file = fileSerializer.read(buffer)
////          inject.getOrElse(file.getPath, throw InternalProcessingError(s"Replacement for file $file not found among $inject"))
////
////      val fileSerializer = fory.getClassResolver.getSerializer(classOf[File])
////      fory.registerSerializer(classOf[File], new FileInjector(fory, fileSerializer, files))
////      fory.deserialize(new ForyInputStream(is)).asInstanceOf[T]
////
////    inject[T](buildFory(), is, files)
//
//  def deserialize[T](file: File)(using NotGiven[T =:= Nothing]): T = file.withFileInputStream(deserialize[T])
//  def deserialize[T](is: InputStream)(using NotGiven[T =:= Nothing]): T = serializer(_.deserialize(new ForyInputStream(is)).asInstanceOf[T])
//
////  def deserializeAndExtractFiles[T](tis: TarArchiveInputStream, deleteFilesOnGC: Boolean)(using TmpDirectory, FileService): T =
////    summon[TmpDirectory].withTmpDir: archiveExtractDir =>
////      tis.extract(archiveExtractDir)
////      val fileReplacement = FileSerialisation.deserialiseFileReplacements(archiveExtractDir, buildFory().serialize, deleteOnGC = deleteFilesOnGC)
////      val contentFile = new File(archiveExtractDir, SerializerService.content)
////      deserializeReplaceFiles[T](contentFile, fileReplacement)
//
//  def serialize(obj: Any, os: OutputStream) = serializer(_.serialize(os, obj))
//
//  def serialize(obj: Any, file: File, gz: Boolean = false): Unit =
//    val os = file.bufferedOutputStream(gz = gz)
//    try serializer(_.serialize(os, obj))
//    finally os.close()
//
////  def serializeAndArchiveFiles(obj: Any, tos: TarArchiveOutputStream)(using TmpDirectory): Unit =
////    summon[TmpDirectory].withTmpFile: objSerial =>
////      serialize(obj, objSerial)
////      tos.addFile(objSerial, SerializerService.content)
////
////    val files = listFiles(obj)
////    FileSerialisation.serialiseFiles(files, tos, (os, obj) => buildFory().serialize(os, obj))
