/*
 * Copyright (C) 07/07/13 Romain Reuillon
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

package org.openmole.ide.core.implementation.serializer

import com.thoughtworks.xstream.mapper.Mapper
import com.thoughtworks.xstream.converters.collections.AbstractCollectionConverter
import com.thoughtworks.xstream.io.{ HierarchicalStreamReader, HierarchicalStreamWriter }
import com.thoughtworks.xstream.converters.{ UnmarshallingContext, MarshallingContext }
import scala.collection.immutable.HashMap

class HashMapConverter(implicit _mapper: Mapper) extends AbstractCollectionConverter(_mapper) {

  def canConvert(clazz: Class[_]) = classOf[HashMap[_, _]] == clazz || HashMap.empty.getClass == clazz

  def marshal(value: Any, writer: HierarchicalStreamWriter, context: MarshallingContext) = {
    val list = value.asInstanceOf[HashMap[_, _]]
    for ((k, v) ← list) {
      writer.startNode("entry")
      writeItem(k, context, writer)
      writeItem(v, context, writer)
      writer.endNode()
    }
  }

  def unmarshal(reader: HierarchicalStreamReader, context: UnmarshallingContext) = {
    val list = new scala.collection.mutable.ListBuffer[(Any, Any)]()
    while (reader.hasMoreChildren()) {
      reader.moveDown()
      val k = readItem(reader, context, list)
      val v = readItem(reader, context, list)
      list += k -> v
      reader.moveUp()
    }
    HashMap() ++ list
  }
}
