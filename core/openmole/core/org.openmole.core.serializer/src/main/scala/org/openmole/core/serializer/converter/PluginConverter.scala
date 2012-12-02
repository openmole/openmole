/*
 * Copyright (C) 2010 Romain Reuillon
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

import com.thoughtworks.xstream.converters.Converter
import com.thoughtworks.xstream.converters.MarshallingContext
import com.thoughtworks.xstream.converters.UnmarshallingContext
import com.thoughtworks.xstream.converters.extended.JavaClassConverter
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter
import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.io.HierarchicalStreamWriter
import org.openmole.misc.pluginmanager.PluginManager
import org.openmole.misc.tools.service.Logger
import org.openmole.core.model.task.ITask
import java.io.File

object PluginConverter extends Logger

import PluginConverter._

class PluginConverter[A <: { def pluginUsed(f: File) }](serializer: A, reflectionConverter: ReflectionConverter) extends Converter {

  override def marshal(o: Object, writer: HierarchicalStreamWriter, mc: MarshallingContext) = {
    if (PluginManager.isClassProvidedByAPlugin(o.getClass)) PluginManager.pluginsForClass(o.getClass).foreach(serializer.pluginUsed)
    if (classOf[ITask].isAssignableFrom(o.getClass)) o.asInstanceOf[ITask].plugins.foreach(serializer.pluginUsed)
    reflectionConverter.marshal(o, writer, mc)
  }

  override def unmarshal(reader: HierarchicalStreamReader, uc: UnmarshallingContext): Object = {
    throw new UnsupportedOperationException("Bug: Should never be called.")
  }

  override def canConvert(c: Class[_]): Boolean =
    classOf[ITask].isAssignableFrom(c) || PluginManager.isClassProvidedByAPlugin(c)

}
