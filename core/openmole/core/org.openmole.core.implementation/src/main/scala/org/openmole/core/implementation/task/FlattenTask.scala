/*
 * Copyright (C) 28/11/12 Romain Reuillon
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

package org.openmole.core.implementation.task

import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.core.implementation.data._
import reflect.ClassTag

object FlattenTask {

  def apply[S, T <: Array[S]](name: String, prototypes: Iterable[Prototype[_ <: T]], result: Prototype[Array[S]])(implicit plugins: PluginSet = PluginSet.empty) =
    new TaskBuilder { builder ⇒

      for (p ← prototypes) addInput(p)
      addOutput(result)

      def toTask =
        new FlattenTask[S, T](name, prototypes, result) with builder.Built
    }

}

sealed abstract class FlattenTask[S, T <: Array[S]](val name: String, prototypes: Iterable[Prototype[_ <: T]], result: Prototype[Array[S]]) extends Task {

  override def process(context: Context) = {
    val flattened = prototypes.map { p ⇒ context(p) }.flatten.toArray[S](ClassTag(result.fromArray.`type`.runtimeClass))
    Variable(result, flattened)
  }

}
