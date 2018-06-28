package org.openmole.plugin.task.timing

import java.io.File

import monocle.macros.Lenses
import org.openmole.core.context. Val
import org.openmole.core.expansion. FromContext
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.task._

object TimingTask {

  implicit def isBuilder: InputOutputBuilder[TimingTask] = InputOutputBuilder(TimingTask.config)
  implicit def isInfo = InfoBuilder(info)

  def apply(
             task: Task,
             tracker:   Val[Long]
           ) = new TimingTask(task, tracker, InputOutputConfig(), InfoConfig())

}

@Lenses case class TimingTask(
                                 task:Task,
                                 tracker:   Val[Long],
                                 config:   InputOutputConfig,
                                 info:     InfoConfig
                               ) extends Task {

  override def inputs = task.inputs
  override def outputs = task.outputs ++ Seq(tracker)

  override protected def process(executionContext: TaskExecutionContext) = FromContext { parameters ⇒
    val starttime = System.currentTimeMillis()
    val taskcontext = task.process(executionContext)(parameters.context)
    val executiontime = (System.currentTimeMillis() - starttime)
    taskcontext + (tracker,executiontime)
  }
}
