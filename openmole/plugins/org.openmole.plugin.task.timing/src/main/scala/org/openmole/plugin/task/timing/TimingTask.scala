package org.openmole.plugin.task.timing

import monocle.Focus
import org.openmole.core.context.Val
import org.openmole.core.argument.FromContext
import org.openmole.core.setter.*
import org.openmole.core.workflow.task.*
import org.openmole.core.workflow.dsl.*
import org.openmole.core.workflow.validation.ValidateTask

object TimingTask:

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

case class TimingTask(
  task:    Task,
  tracker: Val[Long],
  config:  InputOutputConfig,
  info:    InfoConfig
) extends Task:

  override def apply(taskExecutionBuildContext: TaskExecutionBuildContext) =
    val taskExecution = task(taskExecutionBuildContext)
    val taskExecutionInfo = TaskExecutionInfo(task)
    TaskExecution:  p =>
      import p.*
      val starttime = System.currentTimeMillis()
      val taskcontext = p.context + TaskExecution.execute(taskExecution, p.executionContext).from(context)
      val executiontime = (System.currentTimeMillis() - starttime)
      taskcontext + (tracker, executiontime)



