package org.openmole.core.workflow.task

import monocle.macros.Lenses
import org.openmole.core.context.{ Context, Val }
import org.openmole.core.expansion.FromContext
import org.openmole.core.fileservice.FileService
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.task
import org.openmole.core.workflow.validation._
import org.openmole.core.workspace.NewFile
import org.openmole.tool.random.RandomProvider

object FromContextTask {

  implicit def isBuilder: InputOutputBuilder[FromContextTask] = InputOutputBuilder(FromContextTask.config)
  implicit def isInfo = InfoBuilder(FromContextTask.info)

  case class Parameters(context: Context, executionContext: TaskExecutionContext, implicit val random: RandomProvider, implicit val newFile: NewFile, implicit val fileService: FileService)
  case class ValidateParameters(implicit val newFile: NewFile, implicit val fileService: FileService)

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
  v:                      FromContextTask.ValidateParameters ⇒ Seq[Throwable] = _ ⇒ Seq(),
  override val className: String,
  config:                 InputOutputConfig,
  info:                   InfoConfig
) extends Task with ValidateTask {

  override def validate = Validate { p ⇒
    val fcp = FromContextTask.ValidateParameters()(p.newFile, p.fileService)
    v(fcp)
  }

  override protected def process(executionContext: TaskExecutionContext) = FromContext[Context] { p ⇒
    val tp = FromContextTask.Parameters(p.context, executionContext, p.random, p.newFile, p.fileService)
    f(tp)
  }

  def validate(validate: task.FromContextTask.ValidateParameters ⇒ Seq[Throwable]) = {
    def nv(p: task.FromContextTask.ValidateParameters) = v(p) ++ validate(p)
    copy(v = nv)
  }

}