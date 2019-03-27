package org.openmole.plugin.method

import org.openmole.core.context.{ Context, Namespace }
import org.openmole.core.dsl._
import org.openmole.core.expansion._
import org.openmole.plugin.tool.pattern._
import mgo.abc._
import org.openmole.core.workflow.builder.DefinitionScope
import org.openmole.core.expansion.Condition

package object abc {

  val abcNamespace = Namespace("abc")

  case class ABCPrior(v: Val[Double], low: FromContext[Double], high: FromContext[Double])
  case class ABCObserved(v: Val[Double], observed: Double)

  def ABC(
    evaluation:       DSL,
    prior:            Seq[ABCPrior],
    observed:         Seq[ABCObserved],
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
        aggregation = postStepTask,
        scope = scope
      )

    val loop =
      While(
        evaluation = mapReduce,
        condition = !(stop: Condition)
      )

    DSLContainer(loop, output = Some(postStepTask), delegate = mapReduce.delegate)
  }

}
