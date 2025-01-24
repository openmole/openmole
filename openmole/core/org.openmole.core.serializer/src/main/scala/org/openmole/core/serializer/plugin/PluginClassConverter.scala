/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.core.serializer.plugin

import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.converters.extended.JavaClassConverter
import java.io.File
import com.thoughtworks.xstream.core.ClassLoaderReference
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.serializer.PluginAndFilesListing

class PluginClassConverter(serializer: PluginAndFilesListing) extends JavaClassConverter(new ClassLoaderReference(classOf[XStream].getClassLoader)) {

  override def toString(obj: Object) = {
    val c = obj.asInstanceOf[Class[?]]
    serializer.classUsed(c)
    super.toString(obj)
  }

}
