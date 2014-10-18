/*
 * Copyright (C) 2014 Romain Reuillon
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

package org.openmole.ide.core.implementation.serializer

import com.thoughtworks.xstream.converters.{ UnmarshallingContext, MarshallingContext }
import com.thoughtworks.xstream.converters.collections.AbstractCollectionConverter
import com.thoughtworks.xstream.io.{ HierarchicalStreamReader, HierarchicalStreamWriter }
import com.thoughtworks.xstream.mapper.Mapper

class EitherConverter(implicit _mapper: Mapper) extends AbstractCollectionConverter(_mapper) {
  def canConvert(clazz: Class[_]) = classOf[Right[_, _]] == clazz || classOf[Left[_, _]] == clazz || classOf[Either[_, _]] == clazz

  def marshal(value: Any, writer: HierarchicalStreamWriter, context: MarshallingContext) = {
    val either = value.asInstanceOf[Either[_, _]]
    either match {
      case Right(c) ⇒ writeItem(c, context, writer)
      case Left(c)  ⇒ writeItem(c, context, writer)
    }
  }

  def unmarshal(reader: HierarchicalStreamReader, context: UnmarshallingContext) = {
    val c = reader.getAttribute("class")
    reader.moveDown()
    val item = readItem(reader, context, null)
    reader.moveUp()
    c match {
      case "Right"            ⇒ Right(item)
      case "Left"             ⇒ Left(item)
      case "scala.util.Right" ⇒ Right(item)
      case "scala.util.Left"  ⇒ Left(item)
    }
  }
}
