package org.openmole.core.workflow.test

import org.openmole.core.context.Context
import org.openmole.core.setter.DefinitionScope
import org.openmole.core.workflow.task.ClosureTask

object TestTask {

  def apply(f: Context => Context)(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    ClosureTask("TestTask")((ctx, _, _) => f(ctx))

}
