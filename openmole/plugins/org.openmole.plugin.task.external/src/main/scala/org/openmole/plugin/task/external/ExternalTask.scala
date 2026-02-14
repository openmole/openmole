package org.openmole.plugin.task.external

/*
 * Copyright (C) 2025 Romain Reuillon
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

import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*

import monocle.Focus

object ExternalTask:
  given InputOutputBuilder[ExternalTask] = InputOutputBuilder(Focus[ExternalTask](_.config))
  given InfoBuilder[ExternalTask] = InfoBuilder(Focus[ExternalTask](_.info))
  given MappedInputOutputBuilder[ExternalTask] = MappedInputOutputBuilder(Focus[ExternalTask](_.mapped))
  given ExternalBuilder[ExternalTask] = ExternalBuilder(Focus[ExternalTask](_.external))

  case class TaskInfo(
    config:                    InputOutputConfig,
    mapped:                    MappedInputOutputConfig,
    info:                      InfoConfig,
    external:                  External)

  case class BuildParameters(
    taskExecutionBuildContext: TaskExecutionBuildContext,
    taskInfo: TaskInfo):
    export taskInfo.*

  case class Parameters(
   context:          Context,
   executionContext: TaskExecutionContext,
   taskInfo: TaskInfo)(
   implicit
   val random:      RandomProvider,
   val tmpDirectory: TmpDirectory):
   export taskInfo.*
   export executionContext.*

  type BuildFunction = BuildParameters => ExecutionFunction
  type ExecutionFunction = (Parameters => Context, Option[External])

  // --- For XStream along with inline
  trait ExecutionFunctionTrait:
    def external: Option[External]
    def apply(p: Parameters): Context

  trait BuildFunctionTrait:
    def apply(p: BuildParameters): ExecutionFunctionTrait
  // ---


  /**
   * Construct from a [[FromContext.Parameters]] => [[Context]] function
   * @param className
   * @param fromContext
   * @return
   */
  inline def apply(className: String)(inline fromContext: Parameters => Context)(using sourcecode.Name, DefinitionScope): ExternalTask =
    build(className): bp =>
      (fromContext, None)

  inline def build(className: String)(inline fromContext: BuildFunction)(using sourcecode.Name, DefinitionScope): ExternalTask =
    val buildFunction =
      new BuildFunctionTrait:
        override def apply(p: BuildParameters): ExecutionFunctionTrait =
          val (executeFunction, executionExternal) = fromContext(p)
          new ExecutionFunctionTrait:
            override def external: Option[External] = executionExternal
            override def apply(p: Parameters): Context = executeFunction(p)

    new ExternalTask(
      buildFunction,
      className = className,
      config = InputOutputConfig(),
      mapped = MappedInputOutputConfig(),
      info = InfoConfig(),
      external = External(),
      v = _ => Validate.success
    )

  inline def execution(inline f: Parameters => Context): ExecutionFunction = (f, None)

/**
 * A task wrapping a function from a [[TaskExecutionContext]] to a [[FromContext]]
 *
 * @param f execution function
 * @param v validation tests
 * @param className name of the task
 * @param config
 * @param info
 */
case class ExternalTask(
  fromContext: ExternalTask.BuildFunctionTrait,
  v: ExternalTask.TaskInfo => Validate,
  override val className: String,
  config: InputOutputConfig,
  mapped: MappedInputOutputConfig,
  info: InfoConfig,
  external: External) extends Task with ValidateTask:

  override def validate: Validate =
    val taskInfo = ExternalTask.TaskInfo(config, mapped, info, external)
    v(taskInfo)

  override def apply(taskExecutionBuildContext: TaskExecutionBuildContext) =
    val taskInfo = ExternalTask.TaskInfo(config, mapped, info, external)
    val execution = fromContext(ExternalTask.BuildParameters(taskExecutionBuildContext, taskInfo))
    val executionTaskInfo = taskInfo.copy(external = execution.external.getOrElse(taskInfo.external))
    TaskExecution: p =>
      val tp = ExternalTask.Parameters(p.context, p.executionContext, executionTaskInfo)(using p.random, p.tmpDirectory)
      execution(tp)

  def withValidate(validate: Validate): ExternalTask = copy(v = info => v(info) ++ validate)
  def withValidate(validate: ExternalTask.TaskInfo => Validate): ExternalTask = copy(v = info => v(info) ++ validate(info))
