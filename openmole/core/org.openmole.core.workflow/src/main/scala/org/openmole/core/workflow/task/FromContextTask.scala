package org.openmole.core.workflow.task

import monocle.macros.Lenses
import org.openmole.core.context.Context
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.builder.{ InputOutputBuilder, InputOutputConfig }

object FromContextTask {

  implicit def isBuilder: InputOutputBuilder[FromContextTask] = InputOutputBuilder(FromContextTask.config)

  def withTaskExecutionContext(className: String, fromContext: TaskExecutionContext ⇒ FromContext[Context])(implicit name: sourcecode.Name): FromContextTask = new FromContextTask(
    fromContext,
    className = className,
    config = InputOutputConfig()
  )

  def apply(className: String, fromContext: FromContext[Context])(implicit name: sourcecode.Name): FromContextTask =
    withTaskExecutionContext(className, (_: TaskExecutionContext) ⇒ fromContext)

  def apply(className: String)(fromContext: FromContext.Parameters ⇒ Context)(implicit name: sourcecode.Name): FromContextTask =
    withTaskExecutionContext(className, (_: TaskExecutionContext) ⇒ FromContext(fromContext))

}

@Lenses case class FromContextTask(
  fromContext:            TaskExecutionContext ⇒ FromContext[Context],
  override val className: String,
  config:                 InputOutputConfig
) extends Task {
  override protected def process(executionContext: TaskExecutionContext): FromContext[Context] = fromContext(executionContext)
}