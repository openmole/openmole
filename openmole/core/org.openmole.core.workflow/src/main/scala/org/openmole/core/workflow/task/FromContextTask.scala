package org.openmole.core.workflow.task

import monocle.macros.Lenses
import org.openmole.core.context.{ Context, Val }
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.validation._

object FromContextTask {

  implicit def isBuilder: InputOutputBuilder[FromContextTask] = InputOutputBuilder(FromContextTask.config)
  implicit def isInfo = InfoBuilder(FromContextTask.info)

  def withTaskExecutionContext(className: String, fromContext: TaskExecutionContext ⇒ FromContext[Context])(implicit name: sourcecode.Name, definitionScope: DefinitionScope): FromContextTask = new FromContextTask(
    fromContext,
    className = className,
    config = InputOutputConfig(),
    info = InfoConfig()
  )

  /**
   * Construct from a FromContext
   * @param className
   * @param fromContext
   * @return
   */
  def apply(className: String, fromContext: FromContext[Context])(implicit name: sourcecode.Name, definitionScope: DefinitionScope): FromContextTask =
    withTaskExecutionContext(className, (_: TaskExecutionContext) ⇒ fromContext)

  /**
   * Construct from a [[FromContext.Parameters]] => [[Context]] function
   * @param className
   * @param fromContext
   * @return
   */
  def apply(className: String)(fromContext: FromContext.Parameters ⇒ Context)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): FromContextTask =
    withTaskExecutionContext(className, (_: TaskExecutionContext) ⇒ FromContext(fromContext))

}

/**
 * A task wrapping a function from a [[TaskExecutionContext]] to a [[FromContext]]
 *
 * @param fromContext function
 * @param className name of the task
 * @param config
 * @param info
 */
@Lenses case class FromContextTask(
  fromContext:            TaskExecutionContext ⇒ FromContext[Context],
  override val className: String,
  config:                 InputOutputConfig,
  info:                   InfoConfig
) extends Task with ValidateTask {

  override def validate = Validate { p ⇒
    import p._
    fromContext.validate(inputs.toSeq)
  }

  override protected def process(executionContext: TaskExecutionContext): FromContext[Context] = fromContext(executionContext)
}