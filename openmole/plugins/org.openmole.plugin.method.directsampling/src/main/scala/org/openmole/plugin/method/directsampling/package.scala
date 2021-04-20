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
import org.openmole.core.workflow.format.WritableOutput
import org.openmole.core.workflow.hook.FormattedFileHook
import org.openmole.core.workflow.mole
import org.openmole.plugin.sampling.combine._
import org.openmole.plugin.domain.distribution._
import org.openmole.plugin.domain.modifier._
import org.openmole.plugin.tool.pattern._
import org.openmole.plugin.hook.file._
import org.openmole.plugin.hook.omr._

package object directsampling {

  object DirectSamplingMetadata {
    def method = "direct sampling"

    import io.circe._
    import io.circe.generic.extras.auto._
    import io.circe.parser._
    import io.circe.generic.extras.semiauto._
    import org.openmole.plugin.hook.omr._

    implicit def methodData = MethodData[DirectSamplingMetadata](_ ⇒ DirectSamplingMetadata.method)
    implicit def metadataEncoder: Encoder[DirectSamplingMetadata] = deriveConfiguredEncoder[DirectSamplingMetadata]
    implicit def metadataDecoder: Decoder[DirectSamplingMetadata] = deriveConfiguredDecoder[DirectSamplingMetadata]

    case class DirectSampling(sampled: Seq[String], aggregation: Seq[Aggregation]) extends DirectSamplingMetadata
    case class Replication(seed: String, sample: Int, aggregation: Seq[Aggregation]) extends DirectSamplingMetadata

    def aggregation(ag: directsampling.Aggregation) = Aggregation(ag.value.name, ag.outputVal.name)

    case class Aggregation(output: String, aggregated: String)
  }

  sealed trait DirectSamplingMetadata

  case class DirectSamplingMethod(sampled: Seq[Val[_]], aggregation: Seq[Aggregation])
  case class ReplicationMethod(seed: Val[_], sample: Int, aggregation: Seq[Aggregation])

  type Aggregation = AggregateTask.AggregateVal[_, _]

  implicit class DirectSamplingDSL(dsl: DSLContainer[DirectSamplingMethod]) extends DSLContainerHook(dsl) {
    def hook[T](
      output: WritableOutput,
      values: Seq[Val[_]]    = Vector.empty,
      format: T              = CSVOutputFormat(append = true))(implicit outputFormat: OutputFormat[T, DirectSamplingMetadata]): DSLContainer[DirectSamplingMethod] = {
      implicit val defScope = dsl.scope
      val metadata = DirectSamplingMetadata.DirectSampling(dsl.method.sampled.map(_.name), dsl.method.aggregation.map(DirectSamplingMetadata.aggregation))
      dsl hook FormattedFileHook(output = output, values = values, format = format, metadata = metadata)
    }
  }

  implicit class ReplicationDSL(dsl: DSLContainer[ReplicationMethod]) extends DSLContainerHook(dsl) {
    def hook[T](
      output:      WritableOutput,
      values:      Seq[Val[_]]    = Vector.empty,
      includeSeed: Boolean        = false,
      format:      T              = CSVOutputFormat(append = true))(implicit outputFormat: OutputFormat[T, DirectSamplingMetadata]): DSLContainer[ReplicationMethod] = {
      implicit val defScope = dsl.scope
      val exclude = if (!includeSeed) Seq(dsl.method.seed) else Seq()
      val metadata = DirectSamplingMetadata.Replication(seed = dsl.method.seed.name, dsl.method.sample, dsl.method.aggregation.map(DirectSamplingMetadata.aggregation))
      dsl hook FormattedFileHook(output = output, values = values, exclude = exclude, format = format, metadata = metadata)
    }
  }

  def Replication[T: Distribution](
    evaluation:       DSL,
    seed:             Val[T],
    sample:           Int,
    index:            OptionalArgument[Val[Int]] = None,
    distributionSeed: OptionalArgument[Long]     = None,
    aggregation:      Seq[Aggregation]           = Seq.empty,
    wrap:             Boolean                    = false,
    scope:            DefinitionScope            = "replication"
  ) = {
    implicit def defScope = scope

    val exploration =
      index.option match {
        case None        ⇒ ExplorationTask(seed in TakeDomain(UniformDistribution[T](distributionSeed), sample))
        case Some(index) ⇒ ExplorationTask((seed in TakeDomain(UniformDistribution[T](distributionSeed), sample)) withIndex index)
      }

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

    DSLContainerExtension(
      s,
      method =
        ReplicationMethod(
          seed = seed,
          sample = sample,
          aggregation = aggregation
        )
    )
  }

  def DirectSampling[S](
    evaluation:  DSL,
    sampling:    S,
    aggregation: Seq[Aggregation] = Seq(),
    condition:   Condition        = Condition.True,
    wrap:        Boolean          = false,
    scope:       DefinitionScope  = "direct sampling"
  )(implicit isSampling: IsSampling[S]) = {
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

    DSLContainerExtension(
      s,
      method =
        DirectSamplingMethod(
          sampled = isSampling.outputs(sampling).toSeq,
          aggregation = aggregation
        )
    )

  }

}
