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

package org.openmole.plugin.task.stat

import org.openmole.core.implementation.data._
import org.openmole.core.implementation.task.TaskBuilder
import org.openmole.core.model.data.Prototype
import org.openmole.core.model.task.PluginSet
import scala.collection.mutable.ListBuffer

abstract class DoubleSequenceStatTaskBuilder(implicit plugins: PluginSet) extends TaskBuilder { builder ⇒
  private var _sequences = new ListBuffer[(Prototype[Array[Double]], Prototype[Double])]

  def sequences = _sequences.toList

  def addSequence(sequence: Prototype[Array[Double]], stat: Prototype[Double]): this.type = {
    this addInput sequence
    this addOutput stat
    _sequences += sequence -> stat
    this
  }

  trait Built extends super.Built {
    val sequences = builder.sequences
  }

}
