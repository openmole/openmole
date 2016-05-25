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

package org.openmole.core.workflow.task

import monocle.macros.Lenses
import org.openmole.core.workflow.builder.TaskBuilder
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._

object TestTask {

  implicit def isBuilder = new TaskBuilder[TestTask] {
    override def defaults = TestTask.defaults
    override def inputs = TestTask.inputs
    override def name = TestTask.name
    override def outputs = TestTask.outputs
  }

}

@Lenses case class TestTask(
    f:         Context ⇒ Context,
    implicits: Vector[String]    = Vector.empty,
    inputs:    PrototypeSet      = PrototypeSet.empty,
    outputs:   PrototypeSet      = PrototypeSet.empty,
    defaults:  DefaultSet        = DefaultSet.empty,
    name:      Option[String]    = None
) extends Task {
  override protected def process(context: Context, executionContext: TaskExecutionContext)(implicit rng: RandomProvider): Context = f(context)
}
