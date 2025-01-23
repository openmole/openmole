package org.openmole.core.workflow.task

import org.openmole.core.context.{ Context, Val }
import org.openmole.core.argument.{ FromContext, Validate }
import org.openmole.core.fileservice.FileService
import org.openmole.core.preference.Preference
import org.openmole.core.setter._
import org.openmole.core.workflow.task
import org.openmole.core.workflow.validation._
import org.openmole.core.workspace.TmpDirectory
import org.openmole.tool.random.RandomProvider
import monocle.Focus

object FromContextTask:
  given InputOutputBuilder[FromContextTask] = InputOutputBuilder(Focus[FromContextTask](_.config))
  given InfoBuilder[FromContextTask] = InfoBuilder(Focus[FromContextTask](_.info))
  given MappedInputOutputBuilder[FromContextTask] = MappedInputOutputBuilder(Focus[FromContextTask](_.mapped))

  case class TaskInfo(
    config:                    InputOutputConfig,
    mapped:                    MappedInputOutputConfig,
    info:                      InfoConfig)

  case class BuildParameters(
    taskExecutionBuildContext: TaskExecutionBuildContext,
    taskInfo: TaskInfo):
    export taskInfo.*

  case class Parameters(
    context:          Context,
    executionContext: TaskExecutionContext,
    taskInfo: TaskInfo)(
    implicit
    val random:           RandomProvider,
    val tmpDirectory:     TmpDirectory):
    export taskInfo.*
    export executionContext.*

  /**
   * Construct from a [[FromContext.Parameters]] => [[Context]] function
   * @param className
   * @param fromContext
   * @return
   */
  inline def apply(className: String)(inline fromContext: Parameters => Context)(using sourcecode.Name, DefinitionScope): FromContextTask =
    build(className): bp =>
      fromContext

  inline def build(className: String)(inline fromContext: BuildParameters => Parameters => Context)(using sourcecode.Name, DefinitionScope): FromContextTask =
    val buildFunction =
      new BuildFunction:
        override def apply(p: BuildParameters): ExecuteFunction =
          val executeFunction = fromContext(p)
          new ExecuteFunction:
            override def apply(p: Parameters): Context = executeFunction(p)

    new FromContextTask(
      buildFunction,
      className = className,
      config = InputOutputConfig(),
      mapped = MappedInputOutputConfig(),
      info = InfoConfig(),
      v = _ => Validate.success
    )

  inline def execution(inline f: Parameters => Context): Parameters => Context = f

  // --- For XStream along with inline
  trait ExecuteFunction:
    def apply(p: Parameters): Context

  trait BuildFunction:
    def apply(p: BuildParameters): ExecuteFunction
  // ---


/**
 * A task wrapping a function from a [[TaskExecutionContext]] to a [[FromContext]]
 *
 * @param f execution function
 * @param v validation tests
 * @param className name of the task
 * @param config
 * @param info
 */
case class FromContextTask(
  fromContext:            FromContextTask.BuildFunction,
  v:                      FromContextTask.TaskInfo => Validate,
  override val className: String,
  config:                 InputOutputConfig,
  mapped:                 MappedInputOutputConfig,
  info:                   InfoConfig
) extends Task with ValidateTask:

  override def validate: Validate =
    val taskInfo = FromContextTask.TaskInfo(config, mapped, info)
    v(taskInfo)

  override def apply(taskExecutionBuildContext: TaskExecutionBuildContext) =
    val taskInfo = FromContextTask.TaskInfo(config, mapped, info)
    val execution = fromContext(FromContextTask.BuildParameters(taskExecutionBuildContext, taskInfo))
    TaskExecution: p =>
      val tp = FromContextTask.Parameters(p.context, p.executionContext, taskInfo)(p.random, p.tmpDirectory)
      execution(tp)

  def withValidate(validate: Validate): FromContextTask = copy(v = info => v(info) ++ validate)
  def withValidate(validate: FromContextTask.TaskInfo => Validate): FromContextTask = copy(v = info => v(info) ++ validate(info))