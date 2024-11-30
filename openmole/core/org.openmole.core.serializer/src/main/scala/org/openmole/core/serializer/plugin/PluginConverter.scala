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

package org.openmole.core.serializer.plugin

import com.thoughtworks.xstream.converters.Converter
import com.thoughtworks.xstream.converters.MarshallingContext
import com.thoughtworks.xstream.converters.UnmarshallingContext
import com.thoughtworks.xstream.converters.extended.JavaClassConverter
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter
import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.io.HierarchicalStreamWriter
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.compiler.*
import org.openmole.core.fileservice.FileService
import org.openmole.core.serializer.PluginAndFilesListing
import org.openmole.core.workspace.TmpDirectory
import org.openmole.tool.cache.KeyValueCache
import org.openmole.tool.logger.JavaLogger

object PluginConverter extends JavaLogger:
  def canConvert(c: Class[?]): Boolean =
    classOf[Plugins].isAssignableFrom(c) || PluginManager.isClassProvidedByAPlugin(c) || Interpreter.isInterpretedClass(c)


class PluginConverter(serializer: PluginAndFilesListing, reflectionConverter: ReflectionConverter) extends Converter:

  override def marshal(o: Object, writer: HierarchicalStreamWriter, mc: MarshallingContext) =
    serializer.classUsed(o.getClass)
    if classOf[Plugins].isAssignableFrom(o.getClass)
    then
      given TmpDirectory = serializer.tmpDirectory
      given FileService = serializer.fileService
      given KeyValueCache = serializer.cache
      o.asInstanceOf[Plugins].plugins.foreach(serializer.pluginUsed)
    reflectionConverter.marshal(o, writer, mc)

  override def unmarshal(reader: HierarchicalStreamReader, uc: UnmarshallingContext): Object =
    throw new UnsupportedOperationException("Bug: Should never be called.")

  override def canConvert(c: Class[?]): Boolean = PluginConverter.canConvert(c)

