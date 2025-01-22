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

import monocle.Focus
import org.openmole.core.argument.{FromContext, Validate}
import org.openmole.core.context.*
import org.openmole.core.fileservice.FileService
import org.openmole.core.preference.Preference
import org.openmole.core.setter.*
import org.openmole.core.workflow.dsl.*
import org.openmole.core.workflow.*
import org.openmole.core.workflow.validation.*
import org.openmole.core.workspace.TmpDirectory
import org.openmole.tool.random.RandomProvider
import org.openmole.tool.exception.Retry


object RetryTask:
  given InfoBuilder[RetryTask] = InfoBuilder(Focus[RetryTask](_.info))
  given InputOutputBuilder[RetryTask] = InputOutputBuilder(Focus[RetryTask](_.config))

  def apply(task: Task, time: Int)(using sourcecode.Name, DefinitionScope) =
    val tt =
      new RetryTask(
        task,
        time,
        config = task.config,
        info = InfoConfig()
      )

    tt set (
      dsl.inputs ++= task.outputs
    )

case class RetryTask(
  task:                   Task,
  time:                   Int,
  config:                 InputOutputConfig,
  info:                   InfoConfig) extends Task with ValidateTask:

  override def validate: Validate = ValidateTask.validate(task)

  override def apply(taskBuildContext: TaskExecutionBuildContext) =
    val taskProcess = task(taskBuildContext)
    TaskExecution: p =>
      import p.*
      Retry.retry(time):
        TaskExecution.execute(taskProcess, executionContext).from(context)
