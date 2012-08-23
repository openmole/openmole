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

import com.thoughtworks.xstream.XStreamException
import com.thoughtworks.xstream.converters.extended.FileConverter
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter
import org.openmole.core.serializer.structure.FileInfo
import com.thoughtworks.xstream.converters.Converter
import com.thoughtworks.xstream.converters.MarshallingContext
import com.thoughtworks.xstream.converters.UnmarshallingContext
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter
import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.io.HierarchicalStreamWriter
import java.io.File

class FilePathHashNotifier(serializer: SerializerWithPathHashInjection, reflectionConverter: ReflectionConverter) extends Converter {

  override def marshal(o: Object, writer: HierarchicalStreamWriter, mc: MarshallingContext) = {
    val file = o.asInstanceOf[File]
    reflectionConverter.marshal(serializer.fileUsed(file), writer, mc)
  }

  override def unmarshal(reader: HierarchicalStreamReader, uc: UnmarshallingContext): Object = {
    throw new UnsupportedOperationException("Bug: Should never be called.")
  }

  override def canConvert(c: Class[_]): Boolean = classOf[File].isAssignableFrom(c)

}
