/*
 * Copyright (C) 2012 reuillon
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

package org.openmole.core.implementation.task

import java.io.File
import org.openmole.core.model.task.IPluginSet
import org.openmole.misc.pluginmanager.PluginManager

object PluginSet {
  val empty = new PluginSet(Set.empty)
}

class PluginSet(plugins: Set[File]) extends Set[File] with IPluginSet {
  override def empty = PluginSet.empty
  override def -(f: File) = new PluginSet(plugins - f)
  override def +(f: File) = new PluginSet(plugins + f)
  override def contains(f: File) = plugins.contains(f)
  override def iterator = plugins.iterator
}
