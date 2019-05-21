/*
 * Copyright (C) 2013 Romain Reuillon
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

package org.openmole.core.workflow.mole

import org.openmole.core.context.Context
import org.openmole.core.expansion.FromContext
import org.openmole.core.fileservice.FileService
import org.openmole.core.preference.Preference
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.workflow.builder.{ InfoConfig, InputOutputConfig }
import org.openmole.core.workflow.tools._
import org.openmole.core.workspace.{ NewFile, Workspace }
import org.openmole.tool.cache.KeyValueCache
import org.openmole.tool.logger.LoggerService
import org.openmole.tool.outputredirection.OutputRedirection
import org.openmole.tool.random.RandomProvider

case class HookExecutionContext(
  cache:                          KeyValueCache,
  implicit val preference:        Preference,
  implicit val threadProvider:    ThreadProvider,
  implicit val fileService:       FileService,
  implicit val workspace:         Workspace,
  implicit val outputRedirection: OutputRedirection,
  implicit val loggerService:     LoggerService,
  implicit val random:            RandomProvider,
  implicit val newFile:           NewFile)

trait Hook <: Name {

  def config: InputOutputConfig
  def info: InfoConfig
  def inputs = config.inputs
  def outputs = config.outputs
  def defaults = config.defaults
  def name = info.name

  def perform(context: Context, executionContext: HookExecutionContext): Context = {
    import executionContext._
    InputOutputCheck.perform(this, inputs, outputs, defaults, process(executionContext))(preference).from(context)
  }

  protected def process(executionContext: HookExecutionContext): FromContext[Context]
}
