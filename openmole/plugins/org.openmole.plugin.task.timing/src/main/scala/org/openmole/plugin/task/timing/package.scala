
/*
package org.openmole.plugin.task

package timing {

  import org.openmole.core.context._
  import org.openmole.core.workflow.builder.{ DefinitionScope, InputOutputBuilder }
  import org.openmole.core.workflow.task._

  trait TimingTaskPackage {

    implicit def builder: InputOutputBuilder[Task] = InputOutputBuilder(TimingTask.config).asInstanceOf[InputOutputBuilder[Task]]

    implicit class TimingTaskDecorator(task: Task) {
      def withTimer(tracker: Val[Long])(implicit name: sourcecode.Name, definitionScope: DefinitionScope): Task = TimingTask(task, tracker)
    }

  }

}

package object timing extends TimingTaskPackage

*/
