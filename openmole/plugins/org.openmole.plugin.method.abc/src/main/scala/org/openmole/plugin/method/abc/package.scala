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

    @Lenses case class ABCContainer(container: DSLContainer, parameters: ABCParameters, scope: DefinitionScope) {
      def save(directory: FromContext[File]) = {
        implicit val defScope = scope
        this.hook(ABCHook(this, directory))
      }
    }

    def apply(
      evaluation:           DSL,
      prior:                Seq[Prior],
      observed:             Seq[Observed],
      sample:               Int,
      generated:            Int,
      minAcceptedRatio:     OptionalArgument[Double] = 0.01,
      stopSampleSizeFactor: Int                      = 1,
      maxStep:              OptionalArgument[Int]    = None,
      scope:                DefinitionScope          = "abc") = {
      implicit def defScope = scope
      val stepState = Val[MonAPMC.StepState]("stepState", abcNamespace)
      val step = Val[Int]("step", abcNamespace)

      val stop = Val[Boolean]

      val n = sample + generated
      val nAlpha = sample

      val preStepTask = PreStepTask(n, nAlpha, prior, state, stepState, step)
      val postStepTask = PostStepTask(n, nAlpha, stopSampleSizeFactor, prior, observed, state, stepState, minAcceptedRatio, maxStep, stop, step)

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

      ABCContainer(DSLContainer(loop, output = Some(postStepTask), delegate = mapReduce.delegate), ABCParameters(state, step), scope)
    }

  }

  import ABC._

  def IslandABC(
    evaluation:  DSL,
    prior:       Seq[Prior],
    observed:    Seq[Observed],
    sample:      Int,
    generated:   Int,
    parallelism: Int,
    //islandGenerated:      Int                   = 1,
    minAcceptedRatio:     Double                = 0.01,
    stopSampleSizeFactor: Int                   = 1,
    maxStep:              OptionalArgument[Int] = None,
    islandSteps:          Int                   = 1,
    scope:                DefinitionScope       = "abc island"
  ) = {
    implicit def defScope = scope

    val masterState = Val[MonAPMC.MonState]("masterState", abcNamespace)
    val islandState = state

    val step = Val[Int]("step", abcNamespace)
    val stop = Val[Boolean]

    val n = sample + generated
    val nAlpha = sample

    val appendSplit = AppendSplitTask(n, nAlpha, masterState, islandState, step)
    val terminationTask =
      IslandTerminationTask(n, nAlpha, minAcceptedRatio, stopSampleSizeFactor, masterState, step, maxStep, stop) set (
        (inputs, outputs) += islandState.array
      )

    val master =
      MoleTask(appendSplit -- terminationTask) set (
        exploredOutputs += islandState.array
      )

    val slave =
      MoleTask(
        ABC.apply(
          evaluation = evaluation,
          prior = prior,
          observed = observed,
          sample = sample,
          generated = generated,
          minAcceptedRatio = minAcceptedRatio,
          maxStep = islandSteps,
          stopSampleSizeFactor = stopSampleSizeFactor
        )
      )

    val masterSlave =
      MasterSlave(
        SplitTask(masterState, islandState, parallelism),
        master = master,
        slave = slave,
        state = Seq(masterState, step),
        slaves = parallelism,
        stop = stop
      )

    ABCContainer(DSLContainer(masterSlave, output = Some(master), delegate = Vector(slave)), ABCParameters(masterState, step), scope)
  }

}
