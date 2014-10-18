package org.openmole.ide.core.implementation.serializer

import com.thoughtworks.xstream.XStream

/*
* Copyright (C) 2013 Mathieu Mathieu Leclaire <mathieu.Mathieu Leclaire at openmole.org>
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

object XStreamFactory {
  def build = {
    val xstream = new XStream
    implicit val mapper = xstream.getMapper
    xstream.alias("Some", classOf[scala.Some[_]])
    xstream.alias("None", None.getClass)
    xstream.alias("List", classOf[::[_]])
    xstream.alias("List", Nil.getClass)
    xstream.registerConverter(new ListConverter())
    xstream.addImmutableType(Nil.getClass)

    xstream.alias("HashMap", classOf[collection.immutable.HashMap[_, _]])
    xstream.alias("HashMap", classOf[collection.immutable.HashMap.HashMap1[_, _]])
    xstream.alias("HashMap", collection.immutable.HashMap.empty.getClass.asInstanceOf[Class[_]])
    xstream.registerConverter(new HashMapConverter())
    xstream.registerConverter(new Tuple2Converter())

    xstream.alias("Right", classOf[Right[_, _]])
    xstream.alias("Left", classOf[Left[_, _]])

    xstream.registerConverter(new EitherConverter())
    xstream
  }
}
