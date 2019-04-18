package org.openmole.plugin.method

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.plugin.tool.pattern._
import mgo.abc._
import monocle.macros.Lenses

package object abc {

  implicit def abcContainerExtension = DSLContainerExtension(ABC.ABCContainer.container)

  object ABC {
    val abcNamespace = Namespace("abc")
    val state = Val[MonAPMC.MonState]("state", abcNamespace)

    case class Prior(v: Val[Double], low: FromContext[Double], high: FromContext[Double])
    case class Observed(v: Val[Double], observed: Double)
    case class ABCParameters(state: Val[MonAPMC.MonState], step: Val[Int])

    @Lenses case class ABCContainer(container: DSLContainer, parameters: ABCParameters)

    def apply(
      evaluation:           DSL,
      prior:                Seq[Prior],
      observed:             Seq[Observed],
      sample:               Int,
      generated:            Int,
      minAcceptedRatio:     OptionalArgument[Double] = 0.01,
      stopSampleSizeFactor: Int                      = 1,
      termination:          OptionalArgument[Int]    = None,
      scope:                DefinitionScope          = "abc") = {
      implicit def defScope = scope
      val stepState = Val[MonAPMC.StepState]("stepState", abcNamespace)
      val step = Val[Int]("step", abcNamespace)

      val stop = Val[Boolean]

      val n = sample + generated
      val nAlpha = sample

      val preStepTask = PreStepTask(n, nAlpha, prior, state, stepState, step)
      val postStepTask = PostStepTask(n, nAlpha, stopSampleSizeFactor, prior, observed, state, stepState, minAcceptedRatio, termination, stop, step)

      val mapReduce =
        MapReduce(
          sampler = preStepTask,
          evaluation = evaluation,
          aggregation = postStepTask
        )

      val loop =
        While(
          evaluation = mapReduce,
          condition = !(stop: Condition)
        )

      ABCContainer(DSLContainer(loop, output = Some(postStepTask), delegate = mapReduce.delegate), ABCParameters(state, step))
    }

  }

  import ABC._

  def IslandABC(
    evaluation:           DSL,
    prior:                Seq[Prior],
    observed:             Seq[Observed],
    sample:               Int,
    generated:            Int,
    parallelism:          Int,
    islandGenerated:      Int                   = 1,
    minAcceptedRatio:     Double                = 0.01,
    stopSampleSizeFactor: Int                   = 1,
    termination:          OptionalArgument[Int] = None,
    scope:                DefinitionScope       = "abc island"
  ) = {
    implicit def defScope = scope

    val masterState = Val[MonAPMC.MonState]("masterState", abcNamespace)
    val step = Val[Int]("step", abcNamespace)
    val stop = Val[Boolean]

    val n = sample + generated
    val nAlpha = sample

    val appendSplit = AppendSplitTask(n, nAlpha, masterState, state, step)
    val terminationTask = IslandTerminationTask(n, nAlpha, minAcceptedRatio, stopSampleSizeFactor, state, step, termination, stop)
    val master = appendSplit -- terminationTask

    val slave =
      MoleTask(
        apply(
          evaluation = evaluation,
          prior = prior,
          observed = observed,
          sample = sample,
          generated = islandGenerated,
          minAcceptedRatio = None
        )
      )

    val masterSlave = MasterSlave(
      SplitTask(state, masterState, parallelism),
      master = MoleTask(master),
      slave = slave,
      state = Seq(masterState, step),
      slaves = parallelism,
      stop = stop
    )

    ABCContainer(DSLContainer(masterSlave, output = Some(appendSplit), delegate = Vector(slave)), ABCParameters(masterState, step))
  }

}
