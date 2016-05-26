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
import org.openmole.core.workflow.task.ClosureTask

object InputOutputBuilder {

  def apply[T](taskInfo: Lens[T, InputOutputConfig]) = new InputOutputBuilder[T] {
    override def inputs: Lens[T, PrototypeSet] = taskInfo composeLens InputOutputConfig.inputs
    override def defaults: Lens[T, DefaultSet] = taskInfo composeLens InputOutputConfig.defaults
    override def name: Lens[T, Option[String]] = taskInfo composeLens InputOutputConfig.name
    override def outputs: Lens[T, PrototypeSet] = taskInfo composeLens InputOutputConfig.outputs
  }

}

trait InputOutputBuilder[T] extends InputBuilder[T] with OutputBuilder[T] with DefaultBuilder[T] with NameBuilder[T]