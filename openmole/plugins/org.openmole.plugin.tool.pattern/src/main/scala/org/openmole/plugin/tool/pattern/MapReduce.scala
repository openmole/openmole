package org.openmole.plugin.tool.pattern

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

object MapReduce:

  def apply(
    sampler:     Task,
    evaluation:  DSL,
    aggregation: OptionalArgument[DSL] = None,
    condition:   Condition             = Condition.True,
    wrap:        Boolean               = false)(implicit scope: DefinitionScope = "map reduce") =

    val explored = (sampler: DSL).outputs(explore = true)
    val wrapped = org.openmole.plugin.tool.pattern.wrap(evaluation, explored, evaluation.outputs, wrap = wrap)

    aggregation.option match
      case Some(aggregation) =>
        val output = EmptyTask()

        val p =
          (Strain(sampler) -< wrapped when condition) >- aggregation -- output &
            (sampler -- aggregation blockAll wrapped.outputs) &
            (sampler -- Strain(output) blockAll (aggregation.outputs ++ explored))

        DSLContainer(p, (), output = Some(output), delegate = wrapped.delegate)
      case None ⇒
        val p = Strain(sampler) -< Strain(wrapped) when condition
        DSLContainer(p, (), delegate = wrapped.delegate)


