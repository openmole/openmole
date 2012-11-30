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

import java.io.OutputStream
import scala.collection.mutable.HashSet
import java.io.File
import org.openmole.misc.tools.io.FileUtil.fileOrdering
import collection.immutable.TreeSet

class SerializerWithPluginListing extends Serializer {

  var plugins: TreeSet[File] = null

  xstream.registerConverter(new PluginConverter(this, reflectionConverter))
  xstream.registerConverter(new PluginClassConverter(this))

  def pluginUsed(f: File): Unit =
    plugins += f

  def toXMLAndListPluginFiles(obj: Object, outputStream: OutputStream) = {
    plugins = new TreeSet
    xstream.toXML(obj, outputStream)
  }

  def clean: Unit = {
    plugins = null
  }
}
