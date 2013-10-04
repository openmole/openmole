/*
 * Copyright (C) 02/10/13 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.serializer.file

import scala.collection.immutable.TreeSet
import java.io.File
import java.io.OutputStream
import org.openmole.misc.tools.io.FileUtil.fileOrdering
import org.openmole.core.serializer.converter.Serialiser

trait FileListing <: Serialiser {
  private var files: TreeSet[File] = null

  xStream.registerConverter(new FileConverterNotifier(this))

  def fileUsed(file: File) =
    files += file

  def toXMLListFiles(obj: Any, outputStream: OutputStream) = synchronized {
    files = new TreeSet
    xStream.toXML(obj, outputStream)
    val retFiles = files
    files = null
    retFiles
  }

}
