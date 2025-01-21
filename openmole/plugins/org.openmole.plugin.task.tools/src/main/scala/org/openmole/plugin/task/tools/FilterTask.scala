package org.openmole.plugin.task.tools

import org.openmole.core.context.Val
import org.openmole.core.dsl._
import org.openmole.core.setter.DefinitionScope
import org.openmole.core.workflow.task._

object FilterTask:

  def apply(variable: Val[?]*)(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    Task("FilterTask"): p =>
      p.context
    .set (
      (inputs, outputs) ++= variable
    )

