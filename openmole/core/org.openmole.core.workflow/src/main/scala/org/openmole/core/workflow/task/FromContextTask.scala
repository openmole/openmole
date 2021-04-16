package org.openmole.core.workflow.task

import monocle.macros.Lenses
import org.openmole.core.context.{ Context, Val }
import org.openmole.core.expansion.{ FromContext, Validate }
import org.openmole.core.fileservice.FileService
import org.openmole.core.preference.Preference
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.task
import org.openmole.core.workflow.validation._
import org.openmole.core.workspace.TmpDirectory
import org.openmole.tool.random.RandomProvider

object FromContextTask {

  implicit def isBuilder: InputOutputBuilder[FromContextTask] = InputOutputBuilder(FromContextTask.config)
  implicit def isInfo = InfoBuilder(FromContextTask.info)
  implicit def isMapped = MappedInputOutputBuilder(FromContextTask.mapped)

  case class Parameters(
    context:                  Context,
    executionContext:         TaskExecutionContext,
    io:                       InputOutputConfig,
    mapped:                   MappedInputOutputConfig,
    implicit val preference:  Preference,
    implicit val random:      RandomProvider,
    implicit val newFile:     TmpDirectory,
    implicit val fileService: FileService
  )

  /**
   * Construct from a [[FromContext.Parameters]] => [[Context]] function
   * @param className
   * @param fromContext
   * @return
   */
  def apply(className: String)(fromContext: Parameters ⇒ Context)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): FromContextTask =
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
@Lenses case class FromContextTask(
  f:                      FromContextTask.Parameters ⇒ Context,
  v:                      Validate                             = Validate.success,
  override val className: String,
  config:                 InputOutputConfig,
  mapped:                 MappedInputOutputConfig,
  info:                   InfoConfig
) extends Task with ValidateTask {

  override def validate = v

  override protected def process(executionContext: TaskExecutionContext) = FromContext[Context] { p ⇒
    val tp = FromContextTask.Parameters(p.context, executionContext, config, mapped, executionContext.preference, p.random, p.newFile, p.fileService)
    f(tp)
  }

  def withValidate(validate: Validate) = copy(v = v ++ validate)

}