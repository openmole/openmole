/*
 * Copyright (C) 2012 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.method

import org.openmole.core.dsl._
import org.openmole.core.workflow.builder.DefinitionScope
import org.openmole.plugin.domain.distribution._
import org.openmole.plugin.domain.modifier._
import org.openmole.plugin.tool.pattern

package object directsampling {

  implicit def scope = DefinitionScope.Internal

  def Replication[T: Distribution](
    evaluation:       DSL,
    seed:             Val[T],
    replications:     Int,
    distributionSeed: OptionalArgument[Long] = None,
    aggregation:      OptionalArgument[DSL]  = None,
    wrap:             Boolean                = false
  ): DSLContainer =
    DirectSampling(
      evaluation = evaluation,
      sampling = seed in (TakeDomain(UniformDistribution[T](distributionSeed), replications)),
      aggregation = aggregation,
      wrap = wrap
    )

  def DirectSampling[P](
    evaluation:  DSL,
    sampling:    Sampling,
    aggregation: OptionalArgument[DSL] = None,
    condition:   Condition             = Condition.True,
    wrap:        Boolean               = false
  ): DSLContainer = {
    val exploration = ExplorationTask(sampling)
    val wrapped = pattern.wrap(evaluation, sampling.prototypes.toSeq, evaluation.outputs, wrap = wrap)

    aggregation.option match {
      case Some(aggregation) ⇒
        val p =
          (Strain(exploration) -< wrapped when condition) >- aggregation &
            (exploration -- aggregation block (wrapped.outputs: _*))

        DSLContainer(p, delegate = wrapped.delegate)
      case None ⇒
        val p = Strain(exploration) -< Strain(wrapped) when condition
        DSLContainer(p, delegate = wrapped.delegate)
    }
  }

}
