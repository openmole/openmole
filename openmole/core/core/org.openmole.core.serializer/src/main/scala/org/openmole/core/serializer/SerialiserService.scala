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
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.tools.io.TarArchiver._
import org.openmole.core.serializer.structure.PluginClassAndFiles
import org.openmole.core.serializer.converter._
import com.ice.tar.TarOutputStream
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.workspace.Workspace
import com.ice.tar.TarInputStream
import org.openmole.misc.tools.service.LockUtils._
import java.util.concurrent.locks.{ ReentrantReadWriteLock, ReadWriteLock }
import collection.mutable.ListBuffer
import org.openmole.core.serializer.file.{ FileInjection, FileListing, FileSerialisation }
import org.openmole.core.serializer.plugin.PluginListing

object SerialiserService extends Logger with FileSerialisation {

  private val lock = new ReentrantReadWriteLock
  private val xStreamOperations = ListBuffer.empty[(XStream ⇒ _)]

  private val xstream = new XStream
  private val content = "content.xml"

  private trait Initialized extends Factory {
    override def initialize(t: T) = lock.read {
      for {
        op ← xStreamOperations
      } op(t.xStream)
      t
    }
  }

  private val serialiserWithFileListingFactory = new Factory with Initialized {
    type T = Serialiser with FileListing
    def make = new Serialiser with FileListing
  }

  private val pluginListingFactory = new Factory with Initialized {
    type T = Serialiser with PluginListing
    def make = new Serialiser with PluginListing
  }

  private val deserialiserWithFileInjectionFactory = new Factory with Initialized {
    type T = Serialiser with FileInjection
    def make = new Serialiser with FileInjection
  }

  private def xStreams =
    xstream ::
      serialiserWithFileListingFactory.instantiated.map(_.xStream) :::
      pluginListingFactory.instantiated.map(_.xStream) :::
      deserialiserWithFileInjectionFactory.instantiated.map(_.xStream)

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

  def serialiseGetPluginsAndFiles(obj: Any, file: File): PluginClassAndFiles = lock.read {
    val os = file.bufferedOutputStream
    try serialiseGetPluginsAndFiles(obj, os)
    finally os.close
  }

  def serialiseGetPluginsAndFiles(obj: Any, os: OutputStream): PluginClassAndFiles = lock.read {
    val plugins = pluginListingFactory.exec(_.listPlugins(obj))
    val files = serialiserWithFileListingFactory.exec { _.toXMLListFiles(obj, os) }
    PluginClassAndFiles(files, plugins)
  }

  def deserialiseReplaceFiles[T](file: File, files: PartialFunction[File, File]): T = lock.read {
    val is = file.bufferedInputStream
    try deserialiseReplaceFiles[T](is, files)
    finally is.close
  }

  def deserialiseReplaceFiles[T](is: InputStream, files: PartialFunction[File, File]): T =
    lock.read(deserialiserWithFileInjectionFactory.exec {
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

}
