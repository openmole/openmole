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

import com.thoughtworks.xstream.XStreamException
import com.thoughtworks.xstream.converters.extended.FileConverter

class FilePathHashInjecter(deserializer: DeserializerWithFileInjectionFromPathHash) extends FileConverter {

  override def fromString(str: String): Object = {
    val hash = super.fromString(str).asInstanceOf[FileInfoHash]
    val ret = deserializer.getMatchingFile(hash)
    if(ret == null) throw new XStreamException("No matching file for " + hash.toString)
    ret
  }
}
