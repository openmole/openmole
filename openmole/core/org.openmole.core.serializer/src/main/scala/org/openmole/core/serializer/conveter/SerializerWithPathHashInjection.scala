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

package org.openmole.core.serializer.converter

import java.io.File
import org.openmole.misc.tools.io.FileUtil.fileOrdering
import org.openmole.misc.tools.io.StringInputStream
import org.openmole.misc.hashservice.HashService
import scala.collection.immutable.TreeMap
import java.io.OutputStream
import org.openmole.core.serializer.structure.FileInfo

class SerializerWithPathHashInjection extends Serializer {

  var files = new TreeMap[File, FileInfo]
  registerConverter(new FilePathHashNotifier(this, reflectionConverter))
  
  override def toXML(obj: Object, outputStream: OutputStream) = {
    clean
    super.toXML(obj, outputStream)
  }
  
  def fileUsed(file: File) = {
    var hash = files.getOrElse(file,
      {
        val pathHash = HashService.computeHash(new StringInputStream(file.getAbsolutePath))
        val fileHash = HashService.computeHash(file)
            
        val hash = new FileInfo(fileHash, pathHash, file.isDirectory)
        files += file -> hash
        hash
      })
    hash
  }
    
  def clean = {
    files = new TreeMap[File, FileInfo]
  }
    
}
