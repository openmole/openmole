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

object FromContextTask {

  given InputOutputBuilder[FromContextTask] = InputOutputBuilder(Focus[FromContextTask](_.config))
  given InfoBuilder[FromContextTask] = InfoBuilder(Focus[FromContextTask](_.info))
  given MappedInputOutputBuilder[FromContextTask] = MappedInputOutputBuilder(Focus[FromContextTask](_.mapped))

  case class Parameters(
    context:          Context,
    executionContext: TaskExecutionContext,
    io:               InputOutputConfig,
    mapped:           MappedInputOutputConfig)(
    implicit
    val preference:  Preference,
    val random:      RandomProvider,
    val newFile:     TmpDirectory,
    val fileService: FileService
  )

  /**
   * Construct from a [[FromContext.Parameters]] => [[Context]] function
   * @param className
   * @param fromContext
   * @return
   */
  def apply(className: String)(fromContext: Parameters => Context)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): FromContextTask =
    new FromContextTask(
      fromContext,
      className = className,
      config = InputOutputConfig(),
      mapped = MappedInputOutputConfig(),
      info = InfoConfig()
    )

}

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
  f:                      FromContextTask.Parameters => Context,
  v:                      Validate                             = Validate.success,
  override val className: String,
  config:                 InputOutputConfig,
  mapped:                 MappedInputOutputConfig,
  info:                   InfoConfig
) extends Task with ValidateTask:

  override def validate = v

  override protected def process(executionContext: TaskExecutionContext) = FromContext[Context] { p =>
    val tp = FromContextTask.Parameters(p.context, executionContext, config, mapped)(executionContext.preference, p.random, p.tmpDirectory, p.fileService)
    f(tp)
  }

  def withValidate(validate: Validate) = copy(v = v ++ validate)
