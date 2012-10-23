/*
 * Copyright (C) 2012 mathieu
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

import SerializerState._
import com.thoughtworks.xstream.converters.MarshallingContext
import com.thoughtworks.xstream.converters.UnmarshallingContext
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter
import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.io.HierarchicalStreamWriter
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider
import com.thoughtworks.xstream.mapper.Mapper
import org.openmole.ide.core.implementation.dataproxy.TaskDataProxyUI
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI

class TaskConverter(mapper: Mapper,
                    provider: ReflectionProvider,
                    val serializer: GUISerializer,
                    var state: SerializerState) extends ReflectionConverter(mapper, provider) {

  override def marshal(o: Object,
                       writer: HierarchicalStreamWriter,
                       mc: MarshallingContext) = {
    val task = o.asInstanceOf[ITaskDataProxyUI]
    state.content.get(task) match {
      case None ⇒
        state.content += task -> new Serializing(task.id)
        marshal(o, writer, mc)
      case Some(Serializing(id)) ⇒
        state.content(task) = new Serialized(id)
        super.marshal(task, writer, mc)
      case Some(Serialized(id)) ⇒
        writer.addAttribute("id", id.toString)
    }
  }

  override def unmarshal(reader: HierarchicalStreamReader,
                         uc: UnmarshallingContext) = {
    super.unmarshal(reader, uc)
  }

  override def canConvert(t: Class[_]) = t.isAssignableFrom(classOf[TaskDataProxyUI])
}
