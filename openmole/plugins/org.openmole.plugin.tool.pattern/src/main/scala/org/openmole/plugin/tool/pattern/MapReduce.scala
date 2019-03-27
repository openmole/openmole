package org.openmole.plugin.tool.pattern

import org.openmole.core.dsl._
import org.openmole.core.workflow.builder.DefinitionScope
import org.openmole.core.workflow.task.Task

object MapReduce {

  def apply[P](
    sampler:     Task,
    evaluation:  DSL,
    aggregation: OptionalArgument[DSL] = None,
    condition:   Condition             = Condition.True,
    wrap:        Boolean               = false,
    scope:       DefinitionScope       = "map reduce"): DSLContainer = {
    implicit def defScope = scope

    val wrapped = org.openmole.plugin.tool.pattern.wrap(evaluation, (sampler: DSL).outputs(explore = true).toSeq, evaluation.outputs, wrap = wrap)

    aggregation.option match {
      case Some(aggregation) ⇒
        val output = EmptyTask()

        val p =
          (Strain(sampler) -< wrapped when condition) >- aggregation &
            ((sampler -- aggregation block (wrapped.outputs: _*)) -- output) &
            (sampler -- Strain(output) block (aggregation.outputs: _*))

        DSLContainer(p, output = Some(output), delegate = wrapped.delegate)
      case None ⇒
        val p = Strain(sampler) -< Strain(wrapped) when condition
        DSLContainer(p, delegate = wrapped.delegate)
    }
  }

}
