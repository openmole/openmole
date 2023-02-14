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

package org.openmole.plugin.method.directsampling

import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.core.workflow.format.CSVOutputFormat
import org.openmole.plugin.sampling.combine.*
import org.openmole.plugin.domain.distribution.*
import org.openmole.plugin.domain.modifier.*
import org.openmole.plugin.tool.pattern.*
import org.openmole.plugin.hook.file.*
import org.openmole.plugin.hook.omr.*
import org.openmole.plugin.tool.methoddata.*


object DirectSamplingMetadata:
  def method = "direct sampling"

  given MethodData[DirectSamplingMetadata] = MethodData[DirectSamplingMetadata](_ ⇒ DirectSamplingMetadata.method)

  case class DirectSampling(sampled: Seq[ValData], aggregation: Option[Seq[Aggregation]], output: Seq[ValData]) extends DirectSamplingMetadata
  case class Replication(seed: ValData, sample: Int, aggregation: Option[Seq[Aggregation]]) extends DirectSamplingMetadata

  def aggregation(ag: org.openmole.plugin.method.directsampling.Aggregation) = Aggregation(ValData(ag.value), ValData(ag.outputVal))

  case class Aggregation(output: ValData, aggregated: ValData)

sealed trait DirectSamplingMetadata

given CSVOutputFormat.Default[DirectSamplingMetadata] = CSVOutputFormat.Default(append = true)

type Aggregation = AggregateTask.AggregateVal[_, _]

object Replication {

  case class Method(seed: Val[_], sample: Int, aggregation: Seq[Aggregation])

  implicit def method[T]: ExplorationMethod[Replication[T], Method] = r ⇒ {
    implicit def defScope: DefinitionScope = r.scope

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
    implicit def s: DefinitionScope = scope
    index.option match {
      case None        ⇒ ExplorationTask(seed in TakeDomain(UniformDistribution[T](seed = distributionSeed), sample))
      case Some(index) ⇒ ExplorationTask((seed in TakeDomain(UniformDistribution[T](seed = distributionSeed), sample)) withIndex index)
    }
  }
}


implicit class ReplicationHookDecorator[M](t: M)(implicit method: ExplorationMethod[M, Replication.Method]) extends MethodHookDecorator[M, Replication.Method](t) {
  def hook[F](
    output:      WritableOutput,
    values:      Seq[Val[_]]    = Vector.empty,
    includeSeed: Boolean        = false,
    format:      F              = CSVOutputFormat())(implicit outputFormat: OutputFormat[F, DirectSamplingMetadata]): Hooked[M] = {
    val dsl = method(t)
    implicit val defScope: DefinitionScope = dsl.scope
    val exclude = if (!includeSeed) Seq(dsl.method.seed) else Seq()
    val aggregation = if (dsl.method.aggregation.isEmpty) None else Some(dsl.method.aggregation.map(DirectSamplingMetadata.aggregation))
    val metadata = DirectSamplingMetadata.Replication(seed = ValData(dsl.method.seed), dsl.method.sample, aggregation)

    Hooked(t, FormattedFileHook(output = output, values = values, exclude = exclude, format = format, metadata = metadata))
  }
}

object DirectSampling {
  case class Method(sampled: Seq[Val[_]], aggregation: Seq[Aggregation], output: Seq[Val[_]])

  implicit def method[S]: ExplorationMethod[DirectSampling[S], Method] = m ⇒ {
    implicit def defScope: DefinitionScope = m.scope

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
          aggregation = m.aggregation,
          output = s.outputs
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
    implicit def s: DefinitionScope = scope
    ExplorationTask(sampling)
  }

  def sampled = implicitly[IsSampling[S]].apply(sampling).outputs.toSeq
}

implicit class DirectSamplingHookDecorator[M](t: M)(implicit method: ExplorationMethod[M, DirectSampling.Method]) extends MethodHookDecorator[M, DirectSampling.Method](t) {
  def hook[F](
    output: WritableOutput,
    values: Seq[Val[_]]    = Vector.empty,
    format: F              = CSVOutputFormat())(implicit outputFormat: OutputFormat[F, DirectSamplingMetadata]): Hooked[M] = {
    val dsl = method(t)
    implicit val defScope: DefinitionScope = dsl.scope

    val aggregation = if (dsl.method.aggregation.isEmpty) None else Some(dsl.method.aggregation.map(DirectSamplingMetadata.aggregation))
    val metadata = DirectSamplingMetadata.DirectSampling(dsl.method.sampled.map(v ⇒ ValData(v)), aggregation, dsl.method.output.map(v ⇒ ValData(v)))

    Hooked(t, FormattedFileHook(output = output, values = values, format = format, metadata = metadata))
  }
}

