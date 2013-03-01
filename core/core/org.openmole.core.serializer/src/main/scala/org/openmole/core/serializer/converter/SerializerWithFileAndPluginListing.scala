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
import java.io.OutputStream
import org.openmole.misc.tools.io.FileUtil.fileOrdering
import scala.collection.immutable.TreeSet
import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter

class SerializerWithFileAndPluginListing extends Factory.XStreamPool {

  private var files: TreeSet[File] = null
  private var plugins: TreeSet[File] = null

  val fileXStream = new XStream
  fileXStream.registerConverter(new FileConverterNotifier(this))

  val pluginXStream = new XStream
  val reflectionConverter = new ReflectionConverter(pluginXStream.getMapper, pluginXStream.getReflectionProvider)

  pluginXStream.registerConverter(new PluginConverter(this, reflectionConverter))
  pluginXStream.registerConverter(new PluginClassConverter(this))

  val xStreams = List(fileXStream, pluginXStream)

  def pluginUsed(f: File): Unit =
    plugins += f

  def fileUsed(file: File) =
    files += file

  def toXMLAndListPluginFiles(obj: Object, outputStream: OutputStream) = {
    files = new TreeSet
    plugins = new TreeSet

    pluginXStream.toXML(obj, new OutputStream {
      def write(p1: Int) {}
    })

    fileXStream.toXML(obj, outputStream)

    val retFiles = files
    val retPlugins = plugins

    files = null
    plugins = null

    (retFiles, retPlugins)
  }

  def clean {}

}
