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
import org.openmole.core.workflow.mole
import org.openmole.core.workflow.mole.FormattedFileHook
import org.openmole.core.workflow.tools.WritableOutput
import org.openmole.plugin.domain.distribution._
import org.openmole.plugin.domain.modifier._
import org.openmole.plugin.tool.pattern._
import org.openmole.plugin.hook.file._

package object directsampling {

  class DirectSampling
  case class Replication(seed: Val[_])

  type Aggregation = AggregateTask.AggregateVal[_, _]

  implicit class DirectSamplingDSL(dsl: DSLContainer[DirectSampling]) extends DSLContainerHook(dsl) {
    def hook[T](
      output: WritableOutput,
      values: Seq[Val[_]]    = Vector.empty,
      format: T              = CSVOutputFormat(append = true))(implicit outputFormat: OutputFormat[T, DirectSampling]): DSLContainer[DirectSampling] = {
      implicit val defScope = dsl.scope
      dsl hook FormattedFileHook(output = output, values = values, format = format, method = dsl.data)
    }
  }

  implicit class ReplicationDSL(dsl: DSLContainer[Replication]) extends DSLContainerHook(dsl) {
    def hook[T](
      output:      WritableOutput,
      values:      Seq[Val[_]]    = Vector.empty,
      includeSeed: Boolean        = false,
      format:      T              = CSVOutputFormat(append = true))(implicit outputFormat: OutputFormat[T, Replication]): DSLContainer[Replication] = {
      implicit val defScope = dsl.scope
      val exclude = if (!includeSeed) Seq(dsl.data.seed) else Seq()
      dsl hook FormattedFileHook(output = output, values = values, exclude = exclude, format = format, method = dsl.data)
    }
  }

  def Replication[T: Distribution](
    evaluation:       DSL,
    seed:             Val[T],
    replications:     Int,
    distributionSeed: OptionalArgument[Long] = None,
    aggregation:      Seq[Aggregation]       = Seq.empty,
    wrap:             Boolean                = false,
    scope:            DefinitionScope        = "replication"
  ) = {
    implicit def defScope = scope

    val sampling = seed in (TakeDomain(UniformDistribution[T](distributionSeed), replications))
    val exploration = ExplorationTask(sampling)

    val aggregateTask: OptionalArgument[DSL] =
      aggregation match {
        case Seq() ⇒ None
        case s     ⇒ AggregateTask(s)
      }

    val s =
      MapReduce(
        evaluation = evaluation,
        sampler = exploration,
        aggregation = aggregateTask,
        wrap = wrap
      )

    DSLContainerExtension[Replication](s, new Replication(seed))
  }

  def DirectSampling(
    evaluation:  DSL,
    sampling:    Sampling,
    aggregation: Seq[Aggregation] = Seq(),
    condition:   Condition        = Condition.True,
    wrap:        Boolean          = false,
    scope:       DefinitionScope  = "direct sampling"
  ) = {
    implicit def defScope = scope

    val exploration = ExplorationTask(sampling)

    val aggregateTask: OptionalArgument[DSL] =
      aggregation match {
        case Seq() ⇒ None
        case s     ⇒ AggregateTask(s)
      }

    val s =
      MapReduce(
        evaluation = evaluation,
        sampler = exploration,
        condition = condition,
        aggregation = aggregateTask,
        wrap = wrap
      )

    DSLContainerExtension[DirectSampling](s, new DirectSampling)
  }

}
