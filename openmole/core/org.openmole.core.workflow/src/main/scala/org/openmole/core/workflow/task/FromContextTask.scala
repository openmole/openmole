package org.openmole.core.workflow.task

import monocle.macros.Lenses
import org.openmole.core.context.{ Context, Val }
import org.openmole.core.expansion.FromContext
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

  object ValidateParameter {
    implicit def fromServices(implicit newFile: TmpDirectory, fileService: FileService) = ValidateParameter()
  }

  case class ValidateParameter()(implicit val newFile: TmpDirectory, implicit val fileService: FileService)

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
  v:                      FromContextTask.ValidateParameter ⇒ Seq[Throwable] = _ ⇒ Seq(),
  override val className: String,
  config:                 InputOutputConfig,
  mapped:                 MappedInputOutputConfig,
  info:                   InfoConfig
) extends Task with ValidateTask {

  override def validate = Validate { p ⇒
    val fcp = FromContextTask.ValidateParameter()(p.newFile, p.fileService)
    v(fcp)
  }

  override protected def process(executionContext: TaskExecutionContext) = FromContext[Context] { p ⇒
    val tp = FromContextTask.Parameters(p.context, executionContext, config, mapped, executionContext.preference, p.random, p.newFile, p.fileService)
    f(tp)
  }

  def validate(validate: task.FromContextTask.ValidateParameter ⇒ Seq[Throwable]) = {
    def nv(p: task.FromContextTask.ValidateParameter) = v(p) ++ validate(p)
    copy(v = nv)
  }

}