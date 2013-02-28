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

import org.openmole.core.implementation.data._
import org.openmole.core.model.data._
import org.openmole.core.model.task._

object RenameTask {

  def apply(name: String, renamings: (Prototype[T], Prototype[T]) forSome { type T }*)(implicit plugins: PluginSet = PluginSet.empty) =
    new TaskBuilder { builder ⇒

      for ((from, to) ← renamings) {
        addInput(from)
        addOutput(to)
      }

      def toTask =
        new RenameTask(name, renamings: _*) with builder.Built

    }

}
sealed abstract class RenameTask(val name: String, val renamings: (Prototype[T], Prototype[T]) forSome { type T }*) extends Task {

  override def process(context: Context) =
    renamings.map { case (from, to) ⇒ Variable(to, context.valueOrException(from)) }

}
