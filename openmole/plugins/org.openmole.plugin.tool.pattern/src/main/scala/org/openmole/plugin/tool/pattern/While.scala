package org.openmole.plugin.tool.pattern

import org.openmole.core.dsl._
import org.openmole.core.argument.Condition
import org.openmole.core.setter.DefinitionScope

object While:

  def apply(
    evaluation: DSL,
    condition:  Condition)(implicit scope: DefinitionScope = "while"): DSL =
    val last = Strain(EmptyTask())

    (evaluation -- last when !condition) &
      (evaluation -- Slot(evaluation) when condition)

