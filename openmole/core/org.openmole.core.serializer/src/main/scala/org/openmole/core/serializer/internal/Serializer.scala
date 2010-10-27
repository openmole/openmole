/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

package org.openmole.core.serializer.internal

import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.XStreamException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.commons.tools.io.FileInputStream
import org.openmole.commons.tools.io.FileOutputStream
import org.openmole.core.serializer.FileInfoHash
import org.openmole.commons.tools.io.FileUtil._

import org.openmole.core.serializer.ISerializer
import scala.collection.JavaConversions._

class Serializer extends ISerializer {

  val xstream = new XStream

  override def deserialize[T](file: File): T = {
    try {
      val is = new FileInputStream(file)
      try {
        return deserialize(is)
      } finally {
        is.close
      }
    } catch {
      case (ex: IOException) => throw new InternalProcessingError(ex);
    }
  }

  override def deserialize[T](is: InputStream): T = {
    try {
      xstream.fromXML(is).asInstanceOf[T]
    } catch {
      case(ex: XStreamException) => throw new InternalProcessingError(ex)
    }
  }

  override def serializeFilePathAsHashGetPluginClassAndFiles(obj: Object, dir: File): (Iterable[(File, FileInfoHash)], Iterable[Class[_]], File) = {
    try {
      val serializer = SerializerWithPathHashInjectionAndPluginListingFactory.borrowObject
      try {
        val serialized = Activator.getWorkspace.newFile

        val os = new FileOutputStream(serialized)
        try {
          serializer.toXMLAndListPlugableClasses(obj, os)
        } finally {
          os.close
        }

        val hashName = new File(dir, Activator.getHashService.computeHash(serialized).toString)
        if(!hashName.exists) move(serialized, hashName)

        (serializer.files, serializer.classes, hashName)
      } finally {
        SerializerWithPathHashInjectionAndPluginListingFactory.returnObject(serializer)
      }
    } catch {
      case(ex: Exception) => throw new InternalProcessingError(ex)
    }
  }

  override def serializeGetPluginClassAndFiles(obj: Object, file: File): (Iterable[File], Iterable[Class[_]]) = {
    try {
      val os = new FileOutputStream(file)
      try {
        return serializeGetPluginClassAndFiles(obj, os);
      } finally {
        os.close
      }
    } catch {
      case(ex: IOException) => throw new InternalProcessingError(ex)
    }
  }

  override def serializeGetPluginClassAndFiles(obj: Object, os: OutputStream): (Iterable[File], Iterable[Class[_]]) = {
    try {
      val serializer = SerializerWithFileAndPluginListingFactory.borrowObject
      try {
        serializer.toXMLAndListPlugableClasses(obj, os)
        (serializer.files, serializer.classes)
      } finally {
        SerializerWithFileAndPluginListingFactory.returnObject(serializer)
      }
    } catch {
      case (ex: Exception) => throw new InternalProcessingError(ex)
    }
  }

  override def deserializeReplaceFiles[T](file: File, files: PartialFunction[File, File] ): T = {
    try {
      val is = new FileInputStream(file)
      try {
        deserializeReplaceFiles[T](is, files)
      } finally {
        is.close();
      }
    } catch {
      case (ex: IOException) => throw new InternalProcessingError(ex)
    }
  }

  override def deserializeReplaceFiles[T](is: InputStream, files: PartialFunction[File, File]): T = {
    try {
      val serializer = DeserializerWithFileInjectionFromFileFactory.borrowObject
      try {
        serializer.files = files
        serializer.fromXMLInjectFiles[T](is)
      } finally {
        DeserializerWithFileInjectionFromFileFactory.returnObject(serializer)
      }
    } catch {
      case(ex: Exception) => throw new InternalProcessingError(ex)
    }
  }

  override def serialize(obj: Object, os: OutputStream) = {
    try {
      xstream.toXML(obj, os)
    } catch {
      case(ex: XStreamException) => throw new InternalProcessingError(ex)
    }
  }

  override def serialize(obj: Object, file: File): Unit = {
    try {
      val os = new FileOutputStream(file);
      try {
        serialize(obj, os)
      } finally {
        os.close
      }
    } catch {
      case(ex: IOException) => throw new InternalProcessingError(ex)
    }
  }

  override def serializeAsHash(obj: Object, dir: File) = {
    try {
      val serialized = Activator.getWorkspace.newFile
      serialize(obj, serialized)
      val hashName = new File(dir, Activator.getHashService.computeHash(serialized).toString)
      move(serialized, hashName)
    } catch {
      case(ex: IOException) => throw new InternalProcessingError(ex)
    }
  }

  override def deserializeReplacePathHash[T](file: File, files: PartialFunction[FileInfoHash, File]): T = {
    try {
      val is = new FileInputStream(file)
      try {
        deserializeReplacePathHash[T](is, files)
      } finally {
        is.close
      }
    } catch {
      case(ex: IOException) => throw new InternalProcessingError(ex)
    }
  }

  override def deserializeReplacePathHash[T](it: InputStream, files: PartialFunction[FileInfoHash, File]) = {
    try {
      val deserializer = DeserializerWithFileInjectionFromPathHashFactory.borrowObject
      try {
        deserializer.files = files
        deserializer.fromXMLInjectFiles[T](it)
      } finally {
        DeserializerWithFileInjectionFromPathHashFactory.returnObject(deserializer)
      }
    } catch {
      case(ex: Exception) => throw new InternalProcessingError(ex)
    }
  }
}
