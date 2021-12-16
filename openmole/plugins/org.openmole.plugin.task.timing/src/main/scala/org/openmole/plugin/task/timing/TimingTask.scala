package org.openmole.plugin.task.timing

import monocle.Focus
import org.openmole.core.context.Val
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.dsl._

object TimingTask {

  implicit def isBuilder: InputOutputBuilder[TimingTask] = InputOutputBuilder(Focus[TimingTask](_.config))
  implicit def isInfo: InfoBuilder[TimingTask] = InfoBuilder(Focus[TimingTask](_.info))

  def apply(
    task:    Task,
    tracker: Val[Long]
  )(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    new TimingTask(task, tracker, InputOutputConfig(task.inputs, task.outputs), task.info) set (outputs += tracker)

}

case class TimingTask(
  task:    Task,
  tracker: Val[Long],
  config:  InputOutputConfig,
  info:    InfoConfig
) extends Task {

  override def inputs = task.inputs
  override def outputs = task.outputs ++ Seq(tracker)

  override protected def process(executionContext: TaskExecutionContext) = FromContext { parameters ⇒
    val starttime = System.currentTimeMillis()
    val taskcontext = parameters.context + task.perform(parameters.context, executionContext)
    val executiontime = (System.currentTimeMillis() - starttime)
    taskcontext + (tracker, executiontime)
  }
}

