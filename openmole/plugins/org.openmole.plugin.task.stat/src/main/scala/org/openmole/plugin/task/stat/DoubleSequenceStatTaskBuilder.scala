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

package org.openmole.plugin.task.stat

import org.openmole.core.implementation.data._
import org.openmole.core.implementation.task.TaskBuilder
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.task.IPluginSet
import scala.collection.mutable.ListBuffer

abstract class DoubleSequenceStatTaskBuilder(implicit plugins: IPluginSet) extends TaskBuilder {
  private var _sequences = new ListBuffer[(IPrototype[Array[Double]], IPrototype[Double])]
 
  def sequences = _sequences.toList
  
  def addSequence(sequence: IPrototype[Array[Double]], stat: IPrototype[Double]): this.type = {
    this addInput sequence
    this addOutput stat
    _sequences += sequence -> stat
    this
  }

}
