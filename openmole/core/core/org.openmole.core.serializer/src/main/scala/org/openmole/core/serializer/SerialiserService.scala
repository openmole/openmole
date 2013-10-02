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
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.tools.io.TarArchiver._
import org.openmole.core.serializer.structure.PluginClassAndFiles
import java.util.UUID
import org.openmole.core.serializer.converter._
import org.openmole.core.serializer.structure.FileInfo
import com.ice.tar.TarOutputStream
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.workspace.Workspace
import scala.collection.immutable.TreeMap
import com.ice.tar.TarInputStream
import org.openmole.misc.tools.service.LockUtils._
import java.util.concurrent.locks.{ ReentrantReadWriteLock, ReadWriteLock }
import collection.mutable.ListBuffer
import org.openmole.core.serializer.file.FileSerialisation

object SerialiserService extends Logger with FileSerialisation {

  private val lock = new ReentrantReadWriteLock
  private val xStreamOperations = ListBuffer.empty[(XStream ⇒ _)]

  private val xstream = new XStream
  private val content = "content.xml"

  private trait Initialized extends Factory {
    override def initialize(t: T) = lock.read {
      for {
        xs ← t.xStreams
        op ← xStreamOperations
      } op(xs)
      t
    }
  }

  private val serialiserWithPathHashInjectionFactory = new Factory with Initialized {
    type T = SerialiserWithPathHashInjection
    def make = new SerialiserWithPathHashInjection
  }

  private val serialiserWithFileAndPluginListingFactory = new Factory with Initialized {
    type T = SerialiserWithFileAndPluginListing
    def make = new SerialiserWithFileAndPluginListing
  }

  private val deserialiserWithFileInjectionFromFileFactory = new Factory with Initialized {
    type T = DeserialiserWithFileInjectionFromFile
    def make = new DeserialiserWithFileInjectionFromFile
  }

  private val deserialiserWithFileInjectionFromPathHashFactory = new Factory with Initialized {
    type T = DeserialiserWithFileInjectionFromPathHash
    def make = new DeserialiserWithFileInjectionFromPathHash
  }

  private def xStreams =
    xstream ::
      serialiserWithPathHashInjectionFactory.instantiated.flatMap(_.xStreams) :::
      serialiserWithFileAndPluginListingFactory.instantiated.flatMap(_.xStreams) :::
      deserialiserWithFileInjectionFromFileFactory.instantiated.flatMap(_.xStreams) :::
      deserialiserWithFileInjectionFromPathHashFactory.instantiated.flatMap(_.xStreams)

  def register(op: XStream ⇒ Unit) = lock.write {
    xStreamOperations += op
    xStreams.foreach(op)
  }

  def deserialise[T](file: File): T = lock.read {
    val is = new FileInputStream(file)
    try deserialise(is)
    finally is.close
  }

  def deserialise[T](is: InputStream): T = lock.read(xstream.fromXML(is).asInstanceOf[T])

  def deserialiseAndExtractFiles[T](file: File, extractDir: File = Workspace.tmpDir): T = {
    val tis = new TarInputStream(file.bufferedInputStream)
    try deserialiseAndExtractFiles(tis, extractDir)
    finally tis.close
  }

  def deserialiseAndExtractFiles[T](tis: TarInputStream, extractDir: File): T = lock.read {
    val archiveExtractDir = extractDir.newDir("archive")
    tis.extractDirArchiveWithRelativePath(archiveExtractDir)
    val fileReplacement = deserialiseFileReplacements(archiveExtractDir, extractDir)
    val contentFile = new File(archiveExtractDir, content)
    val obj = deserialiseReplaceFiles[T](contentFile, fileReplacement)
    contentFile.delete
    archiveExtractDir.delete
    obj
  }

  def serialiseAndArchiveFiles(obj: Any, f: File): Unit = {
    val os = new TarOutputStream(f.bufferedOutputStream)
    try serialiseAndArchiveFiles(obj, os)
    finally os.close
  }

  def serialiseAndArchiveFiles(obj: Any, tos: TarOutputStream): Unit = lock.read {
    val objSerial = Workspace.newFile
    val serializationResult = serialiseGetPluginsAndFiles(obj, objSerial)
    tos.addFile(objSerial, content)
    objSerial.delete
    serialiseFiles(serializationResult.files, tos)
  }

  def serialiseFilePathAsHashGetFiles(obj: Any, file: File): Map[File, FileInfo] = lock.read {
    val os = file.bufferedOutputStream
    try serialiseFilePathAsHashGetFiles(obj, os)
    finally os.close
  }

  def serialiseFilePathAsHashGetFiles(obj: Any, os: OutputStream): Map[File, FileInfo] =
    lock.read(serialiserWithPathHashInjectionFactory.exec(_.toXML(obj.asInstanceOf[AnyRef], os)))

  def serialiseGetPluginsAndFiles(obj: Any, file: File): PluginClassAndFiles = lock.read {
    val os = file.bufferedOutputStream
    try serialiseGetPluginsAndFiles(obj, os)
    finally os.close
  }

  def serialiseGetPluginsAndFiles(obj: Any, os: OutputStream): PluginClassAndFiles =
    lock.read(serialiserWithFileAndPluginListingFactory.exec {
      serializer ⇒
        val (files, plugins) = serializer.toXMLAndListPluginFiles(obj.asInstanceOf[AnyRef], os)
        new PluginClassAndFiles(files, plugins)
    })

  def deserialiseReplaceFiles[T](file: File, files: PartialFunction[File, File]): T = lock.read {
    val is = file.bufferedInputStream
    try deserialiseReplaceFiles[T](is, files)
    finally is.close
  }

  def deserialiseReplaceFiles[T](is: InputStream, files: PartialFunction[File, File]): T =
    lock.read(deserialiserWithFileInjectionFromFileFactory.exec {
      serializer ⇒
        serializer.files = files
        serializer.fromXML[T](is)
    })

  def serialise(obj: Any) = lock.read(xstream.toXML(obj))

  def serialise(obj: Any, os: OutputStream) = lock.read(xstream.toXML(obj, os))

  def serialise(obj: Any, file: File): Unit = lock.read {
    val os = file.bufferedOutputStream
    try serialise(obj, os)
    finally os.close
  }

  def deserialiseReplacePathHash[T](file: File, files: PartialFunction[FileInfo, File]): T = lock.read {
    val is = file.bufferedInputStream
    try deserialiseReplacePathHash[T](is, files)
    finally is.close
  }

  def deserialiseReplacePathHash[T](is: InputStream, files: PartialFunction[FileInfo, File]) =
    lock.read(deserialiserWithFileInjectionFromPathHashFactory.exec {
      deserialiser ⇒
        deserialiser.files = files
        deserialiser.fromXML[T](is)
    })

}
