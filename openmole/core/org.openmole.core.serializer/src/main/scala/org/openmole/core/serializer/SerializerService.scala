/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
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
import org.openmole.core.serializer.structure.PluginClassAndFiles
import org.openmole.core.serializer.converter._
import org.openmole.core.serializer.structure.FileInfo

object SerializerService {

  private val xstream = new XStream

  def deserialize[T](file: File): T = {
    val is = new FileInputStream(file)
    try deserialize(is)
    finally is.close
  }

  def deserialize[T](is: InputStream): T = xstream.fromXML(is).asInstanceOf[T]
  
  def serializeFilePathAsHashGetFiles(obj: Any, file: File): Map[File,FileInfo] = {
    val os = new FileOutputStream(file)
    try serializeFilePathAsHashGetFiles(obj, os)
    finally os.close
  }

  def serializeFilePathAsHashGetFiles(obj: Any, os: OutputStream): Map[File,FileInfo] = {
    val serializer = SerializerWithPathHashInjectionFactory.borrowObject
    try {
      serializer.toXML(obj.asInstanceOf[AnyRef], os)
      serializer.files
    } finally SerializerWithPathHashInjectionFactory.returnObject(serializer)
  }
  
  
  def serializeGetPluginClassAndFiles(obj: Any, file: File): PluginClassAndFiles = {
    val os = new FileOutputStream(file)
    try serializeGetPluginClassAndFiles(obj, os)
    finally os.close
  }

  def serializeGetPluginClassAndFiles(obj: Any, os: OutputStream): PluginClassAndFiles = {
    val serializer = SerializerWithFileAndPluginListingFactory.borrowObject
    try {
      serializer.toXMLAndListPlugableClasses(obj.asInstanceOf[AnyRef], os)
      new PluginClassAndFiles(serializer.files, serializer.classes)
    } finally SerializerWithFileAndPluginListingFactory.returnObject(serializer)
  }

  def deserializeReplaceFiles[T](file: File, files: PartialFunction[File, File]): T = {
    val is = new FileInputStream(file)
    try deserializeReplaceFiles[T](is, files)
    finally is.close
  }

  def deserializeReplaceFiles[T](is: InputStream, files: PartialFunction[File, File]): T = {
    val serializer = DeserializerWithFileInjectionFromFileFactory.borrowObject
    try {
      serializer.files = files
      serializer.fromXMLInjectFiles[T](is)
    } finally DeserializerWithFileInjectionFromFileFactory.returnObject(serializer)
  }

  def serialize(obj: Any, os: OutputStream) = xstream.toXML(obj, os)

  def serialize(obj: Any, file: File): Unit = {
    val os = new FileOutputStream(file);
    try serialize(obj, os)
    finally os.close
  }

  def deserializeReplacePathHash[T](file: File, files: PartialFunction[FileInfo, File]): T = {
    val is = new FileInputStream(file)
    try deserializeReplacePathHash[T](is, files)
    finally is.close
  }

  def deserializeReplacePathHash[T](is: InputStream, files: PartialFunction[FileInfo, File]) = {
    val deserializer = DeserializerWithFileInjectionFromPathHashFactory.borrowObject
    try {
      deserializer.files = files
      deserializer.fromXMLInjectFiles[T](is)
    } finally DeserializerWithFileInjectionFromPathHashFactory.returnObject(deserializer)
  }
}
