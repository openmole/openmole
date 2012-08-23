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

package org.openmole.plugin.task.serialization

import java.io.File
import org.openmole.core.implementation.task.TaskBuilder
import org.openmole.core.implementation.data._
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.task.IPluginSet
import scala.collection.mutable.ListBuffer

abstract class SerializeXMLTaskBuilder(implicit plugins: IPluginSet) extends TaskBuilder {

  private var _serialize = new ListBuffer[(IPrototype[_], IPrototype[File])]

  def serialize = _serialize.toList

  def serialize(p: IPrototype[_], f: IPrototype[File]) = {
    _serialize += (p -> f)
    this addInput p
    this addOutput f
    this
  }

}
