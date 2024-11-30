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

import org.openmole.core.context.Context
import org.openmole.core.argument.FromContext
import org.openmole.core.setter.*

import monocle.Focus

object EmptyTask:

  given InputOutputBuilder[EmptyTask] = InputOutputBuilder(Focus[EmptyTask](_.config))
  given InfoBuilder[EmptyTask] = InfoBuilder(Focus[EmptyTask](_.info))

  /**
   * The empty Task does nothing ([[EmptyTask]] with identity function)
   * @param name
   * @param definitionScope
   * @return
   */
  def apply()(using sourcecode.Name, DefinitionScope) =
    new EmptyTask(
      InputOutputConfig(),
      InfoConfig()
    )

case class EmptyTask(
  config:                 InputOutputConfig,
  info:                   InfoConfig) extends Task:
  override protected def process(executionContext: TaskExecutionContext): FromContext[Context] =
    FromContext: p =>
      p.context


