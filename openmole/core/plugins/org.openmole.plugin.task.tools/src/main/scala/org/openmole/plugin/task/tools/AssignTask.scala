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

package org.openmole.plugin.task.tools

import org.openmole.core.implementation.data._
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import scala.collection.mutable.ListBuffer
import org.openmole.core.implementation.task._

object AssignTask {

  def apply(name: String)(implicit plugins: PluginSet = PluginSet.empty) =
    new TaskBuilder { builder ⇒

      val toAssign = ListBuffer[(Prototype[T], Prototype[T]) forSome { type T }]()

      def assign[T](from: Prototype[T], to: Prototype[T]) = {
        addInput(from)
        addOutput(to)
        toAssign += ((from, to))
      }

      def toTask =
        new AssignTask(name, toAssign.toList: _*) with builder.Built

    }

}
sealed abstract class AssignTask(val name: String, val renamings: (Prototype[T], Prototype[T]) forSome { type T }*) extends Task {

  override def process(context: Context) =
    renamings.map { case (from, to) ⇒ Variable(to, context(from)) }

}
