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

package org.openmole.core.workflow.builder

import monocle.Lens
import org.openmole.core.workflow.data.{ DefaultSet, PrototypeSet }
import org.openmole.core.workflow.task.{ ClosureTask, TaskConfig }

object TaskBuilder {

  def apply[U]() = new {
    def from[T <: {
      def name: Lens[U, Option[String]]
      def inputs: Lens[U, PrototypeSet]
      def outputs: Lens[U, PrototypeSet]
      def defaults: Lens[U, DefaultSet]
    }](t: T): TaskBuilder[U] = new TaskBuilder[U] {
      override def name = t.name
      override def outputs = t.outputs
      override def inputs = t.inputs
      override def defaults = t.defaults
    }
  }

  def apply[T](taskInfo: Lens[T, TaskConfig]) = new TaskBuilder[T] {
    override def inputs: Lens[T, PrototypeSet] = taskInfo composeLens TaskConfig.inputs
    override def defaults: Lens[T, DefaultSet] = taskInfo composeLens TaskConfig.defaults
    override def name: Lens[T, Option[String]] = taskInfo composeLens TaskConfig.name
    override def outputs: Lens[T, PrototypeSet] = taskInfo composeLens TaskConfig.outputs
  }

}

trait TaskBuilder[T] extends InputOutputBuilder[T] with NameBuilder[T] with Builder[T]
