/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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
package org.openmole.ide.core.implementation.serializer

import com.thoughtworks.xstream.mapper.Mapper
import com.thoughtworks.xstream.converters.collections.AbstractCollectionConverter
import com.thoughtworks.xstream.io.{ HierarchicalStreamReader, HierarchicalStreamWriter }
import com.thoughtworks.xstream.converters.{ UnmarshallingContext, MarshallingContext }

class Tuple2Converter(implicit _mapper: Mapper) extends AbstractCollectionConverter(_mapper) {
  def canConvert(clazz: Class[_]) = classOf[Tuple2[_, _]] == clazz

  def marshal(value: Any, writer: HierarchicalStreamWriter, context: MarshallingContext) = {
    val tuple = value.asInstanceOf[Tuple2[_, _]]
    writeItem(tuple._1, context, writer)
    writeItem(tuple._2, context, writer)
  }

  def unmarshal(reader: HierarchicalStreamReader, context: UnmarshallingContext) = {
    reader.moveDown()
    val k = readItem(reader, context, null)
    reader.moveUp()
    reader.moveDown()
    val v = readItem(reader, context, null)
    reader.moveUp()
    (k, v)
  }
}