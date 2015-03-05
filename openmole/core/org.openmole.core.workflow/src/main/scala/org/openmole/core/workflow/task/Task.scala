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

import org.openmole.core.tools.service.{ Logger, Random }
import org.openmole.core.workflow.data._
import org.openmole.core.serializer.plugin._
import org.openmole.core.workflow.tools._
import org.openmole.core.workspace.{ Workspace, ConfigurationLocation }
import org.openmole.core.tools.service._

object Task extends Logger {
  val OpenMOLEVariablePrefix = new ConfigurationLocation("Task", "OpenMOLEVariablePrefix")

  Workspace += (OpenMOLEVariablePrefix, "oM")

  def prefixedVariable(name: String) = Workspace.preference(OpenMOLEVariablePrefix) + name

  val openMOLESeed = Prototype[Long](prefixedVariable("Seed"))

  def buildRNG(context: Context) = Random.newRNG(context(Task.openMOLESeed))
}

trait Task <: InputOutputCheck {
  /**
   *
   * Perform this task.
   *
   * @param context the context in which the task will be executed
   */
  def perform(context: Context): Context = perform(context, process)

  protected def process(context: Context): Context

  /**
   *
   * Get the name of the task.
   *
   * @return the name of the task
   */
  def name: String

  /**
   *
   * Get the input data of the task.
   *
   * @return the input of the task
   */
  def inputs: DataSet

  /**
   *
   * Get the output data of the task.
   *
   * @return the output data of the task
   */
  def outputs: DataSet

  /**
   *
   * Get all the defaults configured for this task.
   *
   * @return the defaults configured for this task.
   */
  def defaults: DefaultSet

  override def toString: String = name

}

