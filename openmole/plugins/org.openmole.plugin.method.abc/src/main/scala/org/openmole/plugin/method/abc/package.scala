package org.openmole.plugin.method

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.plugin.tool.pattern._
import mgo.abc._
import org.openmole.core.workflow.domain.Bounds
import monocle.macros.Lenses
import org.openmole.core.keyword.In

package object abc {

  object ABC {
    val abcNamespace = Namespace("abc")
    val state = Val[MonAPMC.MonState]("state", abcNamespace)

    object Prior {
      implicit def inToPrior[D](in: In[Val[Double], D])(implicit bounds: Bounds[D, Double]) = Prior(in.value, bounds.min(in.domain), bounds.max(in.domain))
    }

    case class Prior(v: Val[Double], low: FromContext[Double], high: FromContext[Double])

    object Observed {

      object Observable {
        def apply[T](f: T ⇒ Array[Double]) = new Observable[T] {
          def apply(t: T) = f(t)
        }

        implicit def intObservable = Observable[Int](i ⇒ Array(i.toDouble))
        implicit def doubleObservable = Observable[Double](d ⇒ Array(d))
        implicit def iterableDouble = Observable[Iterable[Double]](_.toArray)
        implicit def iterableInt = Observable[Iterable[Int]](i ⇒ i.toArray.map(_.toDouble))
        implicit def arrayDouble = Observable[Array[Double]](identity)
        implicit def arrayInt = Observable[Array[Int]](_.map(_.toDouble))
      }

      trait Observable[T] {
        def apply(t: T): Array[Double]
      }

      implicit def tupleToObserved[T: Observable](t: (Val[T], T)) = Observed(t._1, t._2)

      def fromContext[T](observed: Observed[T], context: Context) = context(observed.v.array).map(v ⇒ observed.obs(v))
      def value[T](observed: Observed[T]) = observed.obs(observed.observed)
    }

    case class Observed[T](v: Val[T], observed: T)(implicit val obs: Observed.Observable[T])

    case class ABCParameters(state: Val[MonAPMC.MonState], step: Val[Int])

    implicit class ABCContainer(dsl: DSLContainer[ABCParameters]) extends DSLContainerHook(dsl) {
      def hook(directory: FromContext[File]): DSLContainer[ABC.ABCParameters] = {
        implicit val defScope = dsl.scope
        dsl hook ABCHook(dsl, directory)
      }
    }

    def apply(
      evaluation:           DSL,
      prior:                Seq[Prior],
      observed:             Seq[Observed[_]],
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

      DSLContainerExtension[ABCParameters](DSLContainer(loop), output = Some(postStepTask), delegate = mapReduce.delegate, data = ABCParameters(state, step))
    }

  }

  import ABC._

  def IslandABC(
    evaluation:  DSL,
    prior:       Seq[Prior],
    observed:    Seq[Observed[_]],
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

    DSLContainerExtension(DSLContainer(masterSlave), output = Some(master), delegate = Vector(slave), data = ABCParameters(masterState, step))
  }

}
