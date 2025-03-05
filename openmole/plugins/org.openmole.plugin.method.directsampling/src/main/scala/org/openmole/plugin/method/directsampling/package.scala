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

import org.openmole.plugin.sampling.combine.*
import org.openmole.plugin.domain.distribution.*
import org.openmole.plugin.domain.modifier.*
import org.openmole.plugin.tool.pattern.*
import org.openmole.plugin.hook.file.*
import io.circe.*

object AggregationMetaData:
  def apply(ag: Aggregation) = new AggregationMetaData(ValData(ag.value), ValData(ag.outputVal))

case class AggregationMetaData(output: ValData, aggregated: ValData) derives derivation.ConfiguredCodec

type Aggregation = AggregateTask.AggregateVal[_, _]

object Replication:
  def methodName = MethodMetaData.name(Replication)

  object MetaData:
    given MethodMetaData[MetaData] = MethodMetaData[MetaData](methodName)

    def apply(m: Method): MetaData =
      val aggregation = if m.aggregation.isEmpty then None else Some(m.aggregation.map(AggregationMetaData.apply))
      MetaData(seed = ValData(m.seed), m.sample, aggregation)

  case class MetaData(seed: ValData, sample: Int, aggregation: Option[Seq[AggregationMetaData]]) derives derivation.ConfiguredCodec

  case class Method(seed: Val[?], sample: Int, aggregation: Seq[Aggregation])

  given [T]: ExplorationMethod[Replication[T], Method] = r =>
    given DefinitionScope = r.scope

    val aggregateTask: OptionalArgument[DSL] =
      r.aggregation match
        case Seq() => None
        case s     => AggregateTask(s)

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

case class Replication[T: Distribution](
  evaluation:       DSL,
  seed:             Val[T],
  sample:           Int,
  index:            OptionalArgument[Val[Int]]          = None,
  distributionSeed: OptionalArgument[FromContext[Long]] = None,
  aggregation:      Seq[Aggregation]                    = Seq.empty,
  wrap:             Boolean                             = false,
  scope:            DefinitionScope                     = "replication"
):
  def exploration =
    implicit def s: DefinitionScope = scope
    index.option match
      case None        => ExplorationTask(seed in TakeDomain(UniformDistribution[T](seed = distributionSeed), sample))
      case Some(index) => ExplorationTask((seed in TakeDomain(UniformDistribution[T](seed = distributionSeed), sample)) withIndex index)


implicit class ReplicationHookDecorator[M](t: M)(implicit method: ExplorationMethod[M, Replication.Method]) extends MethodHookDecorator[M, Replication.Method](t):
  def hook(
    output:      WritableOutput,
    values:      Seq[Val[?]]    = Vector.empty,
    includeSeed: Boolean        = false)(using scriptSourceData: ScriptSourceData): Hooked[M] =
    val dsl = method(t)
    implicit val defScope: DefinitionScope = dsl.scope
    val exclude = if (!includeSeed) Seq(dsl.method.seed) else Seq()
    Hooked(t, FormattedFileHook(output = output, values = values, exclude = exclude, metadata = Replication.MetaData(dsl.method), option = OMROption(append = true)))


object DirectSampling:

  def methodName = MethodMetaData.name(DirectSampling)

  object MetaData:
    def apply(m: Method): MetaData =
      val aggregation = if (m.aggregation.isEmpty) None else Some(m.aggregation.map(AggregationMetaData.apply))
      MetaData(m.sampled.map(v => ValData(v)), aggregation, m.output.map(v => ValData(v)))

    given MethodMetaData[MetaData] = MethodMetaData(methodName)

  case class MetaData(sampled: Seq[ValData], aggregation: Option[Seq[AggregationMetaData]], output: Seq[ValData]) derives derivation.ConfiguredCodec

  case class Method(sampled: Seq[Val[?]], aggregation: Seq[Aggregation], output: Seq[Val[?]])

  given [S]: ExplorationMethod[DirectSampling[S], Method] = m =>
    implicit def defScope: DefinitionScope = m.scope

    val aggregateTask: OptionalArgument[DSL] =
      m.aggregation match
        case Seq() => None
        case s     => AggregateTask(s)

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


case class DirectSampling[S: IsSampling](
  evaluation:  DSL,
  sampling:    S,
  aggregation: Seq[Aggregation] = Seq(),
  condition:   Condition        = Condition.True,
  wrap:        Boolean          = false,
  scope:       DefinitionScope  = "direct sampling"):

  def explorationTask =
    implicit def s: DefinitionScope = scope
    ExplorationTask(sampling)

  def sampled = implicitly[IsSampling[S]].apply(sampling).outputs.toSeq


implicit class DirectSamplingHookDecorator[M](t: M)(implicit method: ExplorationMethod[M, DirectSampling.Method]) extends MethodHookDecorator[M, DirectSampling.Method](t):
  def hook(
    output: WritableOutput,
    values: Seq[Val[?]]    = Vector.empty)(using ScriptSourceData): Hooked[M] =
    val dsl = method(t)
    implicit val defScope: DefinitionScope = dsl.scope
    Hooked(t, FormattedFileHook(output = output, values = values, metadata = DirectSampling.MetaData(dsl.method), option = OMROption(append = true)))


object SingleRun:
  def methodName = MethodMetaData.name(SingleRun)

  object MetaData:
    given MethodMetaData[MetaData] = MethodMetaData[MetaData](methodName)

    def apply(m: Method): MetaData =
      MetaData(input = m.input.map(v => ValData(v)))

  case class MetaData(input: Seq[ValData]) derives derivation.ConfiguredCodec

  case class Method(input: Seq[Val[?]])

  given ExplorationMethod[SingleRun, Method] = r =>
    given DefinitionScope = r.scope

    val firstTask =
      EmptyTask() set (r.input, outputs ++= r.input.map(_.value))

    DSLContainer(
      Strain(firstTask) -- Strain(r.evaluation),
      method =
        Method(
          input = r.input.map(_.value)
        )
    )


case class SingleRun(
   evaluation:  DSL,
   input:       Seq[ValueAssignment.Untyped],
   scope:       DefinitionScope           = "single run")


implicit class SingleRunHookDecorator[M](t: M)(implicit method: ExplorationMethod[M, SingleRun.Method]) extends MethodHookDecorator[M, SingleRun.Method](t):
  def hook(
    output: WritableOutput,
    values: Seq[Val[?]]    = Vector.empty)(using ScriptSourceData): Hooked[M] =
    val dsl = method(t)
    implicit val defScope: DefinitionScope = dsl.scope
    Hooked(t, FormattedFileHook(output = output, values = values, metadata = SingleRun.MetaData(dsl.method), option = OMROption(append = false)))



