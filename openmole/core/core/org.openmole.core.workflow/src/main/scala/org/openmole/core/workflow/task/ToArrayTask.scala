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

package org.openmole.core.workflow.task

import org.openmole.core.workflow.builder.TaskBuilder
import org.openmole.core.workflow.data._

import scala.reflect.ClassTag

object ToArrayTask {

  def apply(prototypes: Prototype[T] forSome { type T }*) =
    new TaskBuilder { builder ⇒

      for (p ← prototypes) {
        addInput(p)
        addOutput(p.toArray)
      }

      def toTask =
        new ToArrayTask(prototypes: _*) with builder.Built
    }

}
sealed abstract class ToArrayTask(val prototypes: Prototype[T] forSome { type T }*) extends Task {

  override def process(context: Context) =
    prototypes.map {
      p ⇒ Variable.unsecure(p.toArray, Array(context(p))(ClassTag(p.`type`.runtimeClass)))
    }

}
