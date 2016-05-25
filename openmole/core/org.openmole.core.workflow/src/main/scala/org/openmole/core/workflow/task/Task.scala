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

import org.openmole.core.tools.service
import org.openmole.core.workflow.data._
import org.openmole.core.serializer.plugin._
import org.openmole.core.workflow.execution._
import org.openmole.core.workflow.tools._
import org.openmole.core.workspace.{ Workspace, ConfigurationLocation }
import org.openmole.core.tools.service._
import org.openmole.tool.logger.Logger
import scala.util.Random
import org.openmole.core.workflow.dsl._

object Task extends Logger {
  val OpenMOLEVariablePrefix = ConfigurationLocation("Task", "OpenMOLEVariablePrefix", "oM")

  Workspace setDefault OpenMOLEVariablePrefix

  def prefixedVariable(name: String) = Workspace.preference(OpenMOLEVariablePrefix) + name

  val openMOLESeed = Prototype[Long](prefixedVariable("Seed"))

  def buildRNG(context: Context): Random = service.Random.newRNG(context(Task.openMOLESeed)).toScala

}

case class TaskExecutionContext(tmpDirectory: File, localEnvironment: LocalEnvironment)

trait Task <: InputOutputCheck with Name {

  /**
   *
   * Perform this task.
   *
   * @param context the context in which the task will be executed
   */
  def perform(context: Context, executionContext: TaskExecutionContext)(implicit rng: RandomProvider = RandomProvider(Task.buildRNG(context))): Context =
    perform(context, process(_, executionContext))

  protected def process(context: Context, executionContext: TaskExecutionContext)(implicit rng: RandomProvider): Context

  /**
   *
   * Get the input data of the task.
   *
   * @return the input of the task
   */
  def inputs: PrototypeSet

  /**
   *
   * Get the output data of the task.
   *
   * @return the output data of the task
   */
  def outputs: PrototypeSet

  /**
   *
   * Get all the defaults configured for this task.
   *
   * @return the defaults configured for this task.
   */
  def defaults: DefaultSet

}

