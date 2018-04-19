/*
 * Copyright (C) 17/02/13 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.workflow.mole

import org.openmole.core.context.Context
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.builder.{ InfoConfig, InputOutputConfig }
import org.openmole.core.workflow.tools._

trait Source <: Name {
  def config: InputOutputConfig
  def info: InfoConfig

  def inputs = config.inputs
  def outputs = config.outputs
  def defaults = config.defaults
  def name = info.name

  protected def process(executionContext: MoleExecutionContext): FromContext[Context]

  def perform(context: Context, executionContext: MoleExecutionContext): Context = {
    implicit val rng = executionContext.services.newRandom
    import executionContext.services.newFile
    import executionContext.services.fileService
    InputOutputCheck.perform(inputs, outputs, defaults, process(executionContext))(executionContext.services.preference).from(context)
  }

}