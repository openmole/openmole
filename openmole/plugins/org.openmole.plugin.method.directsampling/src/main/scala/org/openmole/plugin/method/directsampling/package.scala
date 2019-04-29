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
import org.openmole.core.dsl.extension._
import org.openmole.plugin.domain.distribution._
import org.openmole.plugin.domain.modifier._
import org.openmole.plugin.tool.pattern._
import org.openmole.plugin.hook.file._

package object directsampling {

  class DirectSampling

  implicit class DirectSamplingDSL(dsl: DSLContainer[DirectSampling]) extends DSLContainerHook(dsl) {
    def hook(
      file:       FromContext[File],
      values:     Seq[Val[_]]                           = Vector.empty,
      header:     OptionalArgument[FromContext[String]] = None,
      arrayOnRow: Boolean                               = false): DSLContainer[DirectSampling] = {
      implicit val defScope = dsl.scope
      dsl hook CSVHook(file = file, values = values, header = header, arrayOnRow = arrayOnRow)
    }
  }
  def Replication[T: Distribution](
    evaluation:       DSL,
    seed:             Val[T],
    replications:     Int,
    distributionSeed: OptionalArgument[Long] = None,
    aggregation:      OptionalArgument[DSL]  = None,
    wrap:             Boolean                = false,
    scope:            DefinitionScope        = "replication"
  ) =
    DirectSampling(
      evaluation = evaluation,
      sampling = seed in (TakeDomain(UniformDistribution[T](distributionSeed), replications)),
      aggregation = aggregation,
      wrap = wrap,
      scope = scope
    )

  def DirectSampling(
    evaluation:  DSL,
    sampling:    Sampling,
    aggregation: OptionalArgument[DSL] = None,
    condition:   Condition             = Condition.True,
    wrap:        Boolean               = false,
    scope:       DefinitionScope       = "direct sampling"
  ) = {
    implicit def defScope = scope

    val exploration = ExplorationTask(sampling)

    val s =
      MapReduce(
        evaluation = evaluation,
        sampler = exploration,
        condition = condition,
        aggregation = aggregation,
        wrap = wrap
      )

    DSLContainerExtension[DirectSampling](s, new DirectSampling)
  }

}
