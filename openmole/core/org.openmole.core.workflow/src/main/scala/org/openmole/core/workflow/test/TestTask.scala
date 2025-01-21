package org.openmole.core.workflow.test

import org.openmole.core.context.Context
import org.openmole.core.setter.DefinitionScope
import org.openmole.core.workflow.task.{FromContextTask}

object TestTask:

  def apply(f: Context => Context)(using sourcecode.Name, DefinitionScope) =
    FromContextTask("TestTask"): p =>
      import p.*
      f(context)

