package org.openmole.plugin.task

package timing {

  import org.openmole.core.context._
  import org.openmole.core.workflow.task._

  trait TimingTaskPackage {

    implicit class TimingTaskDecorator(task: Task){
      def withTimer(tracker: Val[Long]): Task = TimingTask(task,tracker)
    }

  }

}

package object timing extends TimingTaskPackage
