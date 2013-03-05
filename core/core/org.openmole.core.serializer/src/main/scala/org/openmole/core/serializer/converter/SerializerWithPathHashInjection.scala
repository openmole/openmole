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

package org.openmole.core.serializer.converter

import java.io.File
import org.openmole.misc.tools.io.FileUtil.fileOrdering
import org.openmole.misc.tools.io.StringInputStream
import org.openmole.misc.fileservice.FileService
import org.openmole.misc.hashservice.HashService
import scala.collection.immutable.TreeMap
import java.io.OutputStream
import org.openmole.core.serializer.structure.FileInfo
import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter

class SerializerWithPathHashInjection extends Factory.XStreamPool {

  val xStream = new XStream
  val reflectionConverter = new ReflectionConverter(xStream.getMapper, xStream.getReflectionProvider)

  private var files: TreeMap[File, FileInfo] = null
  xStream.registerConverter(new FilePathHashNotifier(this, reflectionConverter))

  val xStreams = List(xStream)

  def toXML(obj: Object, outputStream: OutputStream) = {
    files = new TreeMap[File, FileInfo]
    xStream.toXML(obj, outputStream)
    val retFiles = files
    files = null
    retFiles
  }

  def fileUsed(file: File) = {
    var hash = files.getOrElse(file,
      {
        val pathHash = HashService.computeHash(new StringInputStream(file.getAbsolutePath))
        val fileHash = FileService.hash(file)

        val hash = new FileInfo(fileHash, pathHash, file.isDirectory)
        files += file -> hash
        hash
      })
    hash
  }

  def clean {}

}
