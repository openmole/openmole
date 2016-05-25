/*
 * Copyright (C) 2011 Romain Reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.workflow.task

import monocle.Lens
import monocle.macros.Lenses
import org.openmole.core.workflow.builder.TaskBuilder
import org.openmole.core.workflow.data._

object EmptyTask {

  def apply() = new EmptyTask()

  implicit def builder = new TaskBuilder[EmptyTask] {
    override def defaults = EmptyTask.defaults
    override def inputs = EmptyTask.inputs
    override def name = EmptyTask.name
    override def outputs = EmptyTask.outputs
  }

}

@Lenses case class EmptyTask(
    inputs:   PrototypeSet   = PrototypeSet.empty,
    outputs:  PrototypeSet   = PrototypeSet.empty,
    defaults: DefaultSet     = DefaultSet.empty,
    name:     Option[String] = None
) extends Task {

  override def process(context: Context, executionContext: TaskExecutionContext)(implicit rng: RandomProvider) = context

}
