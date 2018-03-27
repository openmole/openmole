package org.openmole.plugin.task.tools

import org.openmole.core.context.{ Val }
import org.openmole.core.dsl._
import org.openmole.core.workflow.task._

object FilterTask {

  def apply(variable: Val[T] forSome { type T }*)(implicit name: sourcecode.Name) =
    ClosureTask("FilterTask") { (context, _, _) ⇒ context } set (
      (inputs, outputs) += (variable: _*)
    )

}
