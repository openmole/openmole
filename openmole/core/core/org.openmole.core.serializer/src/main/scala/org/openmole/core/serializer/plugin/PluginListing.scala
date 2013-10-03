/*
 * Copyright (C) 02/10/13 Romain Reuillon
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

package org.openmole.core.serializer.plugin

import scala.collection.immutable.TreeSet
import java.io.{ OutputStream, File }
import org.openmole.core.serializer.converter.Serialiser

trait PluginListing { this: Serialiser ⇒
  private var plugins: TreeSet[File] = null

  xStream.registerConverter(new PluginConverter(this, reflectionConverter))
  xStream.registerConverter(new PluginClassConverter(this))

  def pluginUsed(f: File): Unit =
    plugins += f

  def listPlugins(obj: Any) = synchronized {
    plugins = new TreeSet

    xStream.toXML(obj, new OutputStream {
      def write(p1: Int) {}
    })

    val retPlugins = plugins
    plugins = null
    retPlugins
  }

}
