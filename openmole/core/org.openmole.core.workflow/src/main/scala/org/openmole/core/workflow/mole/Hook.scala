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
import org.openmole.core.workflow.builder.InputOutputConfig
import org.openmole.core.workflow.tools._

trait Hook <: Name {
  def config: InputOutputConfig
  def inputs = config.inputs
  def outputs = config.outputs
  def defaults = config.defaults
  def name = config.name

  def perform(context: Context, executionContext: MoleExecutionContext): Context = {
    val rng = executionContext.newRandom
    InputOutputCheck.perform(inputs, outputs, defaults, process(executionContext))(executionContext.preference).from(context)(rng, executionContext.newFile)
  }

  protected def process(executionContext: MoleExecutionContext): FromContext[Context]
}