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

package org.openmole.core.model.task

import java.io.File
import scala.collection.SetLike

object PluginSet {
  val empty = PluginSet(Set.empty)

  def apply(p: Traversable[File]) = new PluginSet {
    val plugins = p.toSet
  }

  def apply(p: File*): PluginSet = PluginSet(p)

}

trait PluginSet extends Set[File] with SetLike[File, PluginSet] {
  def plugins: Set[File]

  override def empty = PluginSet.empty
  override def -(f: File) = PluginSet(plugins - f)
  override def +(f: File) = PluginSet(plugins + f)
  override def contains(f: File) = plugins.contains(f)
  override def iterator = plugins.iterator
}

