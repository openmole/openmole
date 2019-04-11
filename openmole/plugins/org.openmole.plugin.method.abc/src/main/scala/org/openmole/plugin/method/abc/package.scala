package org.openmole.plugin.method

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.plugin.tool.pattern._
import mgo.abc._
import monocle.macros.Lenses

package object abc {

  implicit def saltelliExtension = DSLContainerExtension(ABC.ABCContainer.container)

  object ABC {
    val abcNamespace = Namespace("abc")

    case class Prior(v: Val[Double], low: FromContext[Double], high: FromContext[Double])
    case class Observed(v: Val[Double], observed: Double)
    case class ABCParameters(state: Val[MonAPMC.MonState], step: Val[Int])

    @Lenses case class ABCContainer(container: DSLContainer, parameters: ABCParameters)

    def apply(
      evaluation:       DSL,
      prior:            Seq[Prior],
      observed:         Seq[Observed],
      sample:           Int,
      generated:        Int,
      minAcceptedRatio: Double                = 0.01,
      termination:      OptionalArgument[Int] = None,
      scope:            DefinitionScope       = "abc") = {
      implicit def defScope = scope
      val state = Val[MonAPMC.MonState]("state", abcNamespace)
      val stepState = Val[MonAPMC.StepState]("stepState", abcNamespace)
      val step = Val[Int]("step", abcNamespace)

      val stop = Val[Boolean]

      val n = sample + generated
      val nAlpha = sample

      val preStepTask = PreStepTask(n, nAlpha, prior, state, stepState, step)
      val postStepTask = PostStepTask(n, nAlpha, prior, observed, state, stepState, minAcceptedRatio, termination, stop, step)

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

}
