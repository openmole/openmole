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

package org.openmole.core.workflow.hook

import org.openmole.core.context.Context
import org.openmole.core.expansion.{DefaultSet, FromContext}
import org.openmole.core.fileservice.FileService
import org.openmole.core.preference.Preference
import org.openmole.core.serializer.SerializerService
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.workflow.builder.{InfoConfig, InputOutputConfig}
import org.openmole.core.workflow.mole.Ticket
import org.openmole.core.workflow.tools.*
import org.openmole.core.workspace.{TmpDirectory, Workspace}
import org.openmole.tool.cache.KeyValueCache
import org.openmole.tool.logger.LoggerService
import org.openmole.tool.outputredirection.OutputRedirection
import org.openmole.tool.random.RandomProvider

case class HookExecutionContext(
  cache:  KeyValueCache,
  ticket: Ticket)(
  implicit
  val preference:        Preference,
  val threadProvider:    ThreadProvider,
  val fileService:       FileService,
  val workspace:         Workspace,
  val outputRedirection: OutputRedirection,
  val loggerService:     LoggerService,
  val random:            RandomProvider,
  val newFile:           TmpDirectory,
  val serializerService: SerializerService)

trait Hook extends Name {

  def config: InputOutputConfig
  def info: InfoConfig
  def inputs = config.inputs ++ DefaultSet.defaultVals(config.inputs, defaults)
  def outputs = config.outputs
  def defaults = config.defaults
  def name = info.name

  def perform(context: Context, executionContext: HookExecutionContext): Context = {
    import executionContext._
    InputOutputCheck.perform(this, inputs, outputs, defaults, process(executionContext))(preference).from(context)
  }

  protected def process(executionContext: HookExecutionContext): FromContext[Context]
}
