package org.openmole.plugin.task.timing

import monocle.Focus
import org.openmole.core.context.Val
import org.openmole.core.expansion.FromContext
import org.openmole.core.setter._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.dsl._

object TimingTask {

  implicit def isBuilder: InputOutputBuilder[TimingTask] = InputOutputBuilder(Focus[TimingTask](_.config))
  implicit def isInfo: InfoBuilder[TimingTask] = InfoBuilder(Focus[TimingTask](_.info))

  def apply(
    task:    Task,
    tracker: Val[Long]
  )(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    new TimingTask(
      task,
      tracker,
      InputOutputConfig(),
      task.info
    ) set (
      inputs ++= Task.inputs(task),
      outputs ++= Task.outputs(task) ++ Seq(tracker)
    )

}

case class TimingTask(
  task:    Task,
  tracker: Val[Long],
  config:  InputOutputConfig,
  info:    InfoConfig
) extends Task {

  override protected def process(executionContext: TaskExecutionContext) = FromContext { parameters ⇒
    val starttime = System.currentTimeMillis()
    val taskcontext = parameters.context + Task.perform(task, parameters.context, executionContext)
    val executiontime = (System.currentTimeMillis() - starttime)
    taskcontext + (tracker, executiontime)
  }
}

