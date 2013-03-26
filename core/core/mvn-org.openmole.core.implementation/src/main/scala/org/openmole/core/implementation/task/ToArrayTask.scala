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

object ToArrayTask {

  def apply(name: String, prototypes: Prototype[T] forSome { type T }*)(implicit plugins: PluginSet = PluginSet.empty) =
    new TaskBuilder { builder ⇒

      for (p ← prototypes) {
        addInput(p)
        addOutput(p.toArray)
      }

      def toTask =
        new ToArrayTask(name, prototypes: _*) {
          val inputs = builder.inputs
          val outputs = builder.outputs
          val parameters = builder.parameters
        }
    }

}
sealed abstract class ToArrayTask(val name: String, val prototypes: Prototype[T] forSome { type T }*)(implicit val plugins: PluginSet) extends Task {

  override def process(context: Context) =
    prototypes.map {
      p ⇒ Variable(p.toArray.asInstanceOf[Prototype[Any]], Array(context(p))(ClassTag(p.`type`.runtimeClass)))
    }

}
