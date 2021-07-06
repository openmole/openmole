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

  type Aggregation = AggregateTask.AggregateVal[_, _]

  object Replication {

    case class Method(seed: Val[_], sample: Int, aggregation: Seq[Aggregation])

    implicit def method[T]: ExplorationMethod[Replication[T], Method] = r ⇒ {
      implicit def defScope = r.scope

      val aggregateTask: OptionalArgument[DSL] =
        r.aggregation match {
          case Seq() ⇒ None
          case s     ⇒ AggregateTask(s)
        }

      val s =
        MapReduce(
          evaluation = r.evaluation,
          sampler = r.exploration,
          aggregation = aggregateTask,
          wrap = r.wrap
        )

      DSLContainer(
        s,
        method =
          Method(
            seed = r.seed,
            sample = r.sample,
            aggregation = r.aggregation
          )
      )
    }
  }

  case class Replication[T: Distribution](
    evaluation:       DSL,
    seed:             Val[T],
    sample:           Int,
    index:            OptionalArgument[Val[Int]]          = None,
    distributionSeed: OptionalArgument[FromContext[Long]] = None,
    aggregation:      Seq[Aggregation]                    = Seq.empty,
    wrap:             Boolean                             = false,
    scope:            DefinitionScope                     = "replication"
  ) {
    def exploration = {
      implicit def s = scope
      index.option match {
        case None        ⇒ ExplorationTask(seed in TakeDomain(UniformDistribution[T](distributionSeed), sample))
        case Some(index) ⇒ ExplorationTask((seed in TakeDomain(UniformDistribution[T](distributionSeed), sample)) withIndex index)
      }
    }
  }

  implicit class ReplicationHookDecorator[M](t: M)(implicit method: ExplorationMethod[M, Replication.Method]) extends MethodHookDecorator[M, Replication.Method](t) {
    def hook[F](
      output:      WritableOutput,
      values:      Seq[Val[_]]    = Vector.empty,
      includeSeed: Boolean        = false,
      format:      F              = CSVOutputFormat(append = true))(implicit outputFormat: OutputFormat[F, DirectSamplingMetadata]): Hooked[M] = {
      val dsl = method(t)
      implicit val defScope = dsl.scope
      val exclude = if (!includeSeed) Seq(dsl.method.seed) else Seq()
      val metadata = DirectSamplingMetadata.Replication(seed = dsl.method.seed.name, dsl.method.sample, dsl.method.aggregation.map(DirectSamplingMetadata.aggregation))
      Hooked(t, FormattedFileHook(output = output, values = values, exclude = exclude, format = format, metadata = metadata))
    }
  }

  object DirectSampling {
    case class Method(sampled: Seq[Val[_]], aggregation: Seq[Aggregation])

    implicit def method[S]: ExplorationMethod[DirectSampling[S], Method] = m ⇒ {
      implicit def defScope = m.scope

      val aggregateTask: OptionalArgument[DSL] =
        m.aggregation match {
          case Seq() ⇒ None
          case s     ⇒ AggregateTask(s)
        }

      val s =
        MapReduce(
          evaluation = m.evaluation,
          sampler = m.explorationTask,
          condition = m.condition,
          aggregation = aggregateTask,
          wrap = m.wrap
        )

      DSLContainer(
        s,
        method =
          Method(
            sampled = m.sampled,
            aggregation = m.aggregation
          )
      )

    }
  }

  case class DirectSampling[S: IsSampling](
    evaluation:  DSL,
    sampling:    S,
    aggregation: Seq[Aggregation] = Seq(),
    condition:   Condition        = Condition.True,
    wrap:        Boolean          = false,
    scope:       DefinitionScope  = "direct sampling") {

    def explorationTask = {
      implicit def s = scope
      ExplorationTask(sampling)
    }

    def sampled = implicitly[IsSampling[S]].outputs(sampling).toSeq
  }

  implicit class DirectSamplingHookDecorator[M](t: M)(implicit method: ExplorationMethod[M, DirectSampling.Method]) extends MethodHookDecorator[M, DirectSampling.Method](t) {
    def hook[F](
      output: WritableOutput,
      values: Seq[Val[_]]    = Vector.empty,
      format: F              = CSVOutputFormat(append = true))(implicit outputFormat: OutputFormat[F, DirectSamplingMetadata]): Hooked[M] = {
      val dsl = method(t)
      implicit val defScope = dsl.scope
      val metadata = DirectSamplingMetadata.DirectSampling(dsl.method.sampled.map(_.name), dsl.method.aggregation.map(DirectSamplingMetadata.aggregation))
      def fileName = if (outputFormat.appendable(format)) None else Some("experiment${" + FormattedFileHook.experiment.name + "}")
      Hooked(t, FormattedFileHook(output = output, values = values, format = format, metadata = metadata, fileName = fileName))
    }
  }

}
