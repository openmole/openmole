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
import org.openmole.core.expansion.FromContext
import org.openmole.core.fileservice.FileService
import org.openmole.core.outputredirection._
import org.openmole.core.preference.Preference
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.workflow.builder.InputOutputConfig
import org.openmole.core.workflow.execution._
import org.openmole.core.workflow.tools._
import org.openmole.core.workspace.{ NewFile, Workspace }
import org.openmole.tool.cache._
import org.openmole.tool.lock._
import org.openmole.tool.random
import org.openmole.tool.random._
import org.openmole.tool.thread._

case class TaskExecutionContext(
  tmpDirectory:                   File,
  localEnvironment:               LocalEnvironment,
  implicit val preference:        Preference,
  implicit val threadProvider:    ThreadProvider,
  fileService:                    FileService,
  implicit val workspace:         Workspace,
  implicit val outputRedirection: OutputRedirection,
  cache:                          KeyValueCache,
  lockRepository:                 LockRepository[LockKey]
)

object Task {
  def buildRNG(context: Context): scala.util.Random = random.Random(context(Variable.openMOLESeed)).toScala
}

trait Task <: Name {

  /**
   *
   * Perform this task.
   *
   * @param context the context in which the task will be executed
   */
  def perform(context: Context, executionContext: TaskExecutionContext): Context = {
    lazy val rng = Lazy(Task.buildRNG(context))
    InputOutputCheck.perform(inputs, outputs, defaults, process(executionContext))(executionContext.preference).from(context)(rng, NewFile(executionContext.tmpDirectory), executionContext.fileService)
  }

  protected def process(executionContext: TaskExecutionContext): FromContext[Context]

  def config: InputOutputConfig

  def inputs = config.inputs
  def outputs = config.outputs
  def defaults = config.defaults
  def name = config.name

}

