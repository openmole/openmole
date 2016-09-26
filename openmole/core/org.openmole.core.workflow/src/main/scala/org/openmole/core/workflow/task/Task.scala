/*
 * Copyright (C) 2010 Romain Reuillon
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

import java.io.File

import org.openmole.core.context._
import org.openmole.core.workflow.builder.InputOutputConfig
import org.openmole.core.workflow.execution._
import org.openmole.core.workflow.tools._
import org.openmole.tool.random._

case class TaskExecutionContext(tmpDirectory: File, localEnvironment: LocalEnvironment)

trait Task <: InputOutputCheck with Name {

  /**
   *
   * Perform this task.
   *
   * @param context the context in which the task will be executed
   */
  def perform(context: Context, executionContext: TaskExecutionContext)(implicit rng: RandomProvider = RandomProvider(Context.buildRNG(context))): Context =
    perform(context, process(_, executionContext))

  protected def process(context: Context, executionContext: TaskExecutionContext)(implicit rng: RandomProvider): Context

  def config: InputOutputConfig

  def inputs = config.inputs
  def outputs = config.outputs
  def defaults = config.defaults
  def name = config.name

}

