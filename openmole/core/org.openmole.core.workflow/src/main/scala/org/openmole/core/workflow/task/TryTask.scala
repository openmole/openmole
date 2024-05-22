package org.openmole.core.workflow.task

/*
 * Copyright (C) 2024 Romain Reuillon
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

import org.openmole.core.context.*
import org.openmole.core.argument.{FromContext, Validate}
import org.openmole.core.fileservice.FileService
import org.openmole.core.preference.Preference
import org.openmole.core.setter._
import org.openmole.core.workflow.task
import org.openmole.core.workflow.*
import org.openmole.core.workflow.dsl.*
import org.openmole.core.setter.*
import org.openmole.core.workflow.validation.*
import org.openmole.core.workspace.TmpDirectory
import org.openmole.tool.random.RandomProvider
import monocle.Focus



object TryTask:
  given InfoBuilder[TryTask] = InfoBuilder(Focus[TryTask](_.info))
  given InputOutputBuilder[TryTask] = InputOutputBuilder(Focus[TryTask](_.config))

  def apply(task: Task)(using sourcecode.Name, DefinitionScope) =
    val tt =
      new TryTask(
      task,
      config = task.config,
      info = InfoConfig()
    )

    tt set (
      dsl.inputs ++= task.outputs
    )

case class TryTask(
  task:                   Task,
  config:                 InputOutputConfig,
  info:                   InfoConfig) extends Task with ValidateTask:

  override def validate: Validate = ValidateTask.validate(task)

  override protected def process(executionContext: TaskExecutionContext): FromContext[Context] = FromContext: p =>
    import p._
    try Task.process(task, executionContext).from(context)
    catch
      case t: Throwable =>
        p.context
