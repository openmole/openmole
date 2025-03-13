package org.openmole.plugin.method

import mgo.abc._
import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.keyword.In
import org.openmole.core.tools.math._
import org.openmole.core.workflow.domain.BoundedFromContextDomain
import org.openmole.core.workflow.task.FromContextTask
import org.openmole.plugin.tool.pattern._
import org.openmole.tool.random.RandomProvider

package object abc {

  object ABC {
    val abcNamespace = Namespace("abc")
    val state = Val[MonAPMC.MonState]("state", abcNamespace)

    object Observed {

      object Observable {
        given Observable[Int] = i => Array(i.toDouble)
        given Observable[Double] = d => Array(d)
        given arrayDouble: Observable[Array[Double]] = identity
        given arrayInt: Observable[Array[Int]] = i => i.map(_.toDouble)
      }

      @FunctionalInterface trait Observable[T] {
        def apply(t: T): Array[Double]
      }

      implicit def tupleIntToObserved(t: (Val[Int], Int)): Observed[Int] = Observed(t._1, t._2)
      implicit def tupleDoubleToObserved(t: (Val[Double], Double)): Observed[Double] = Observed(t._1, t._2)
      implicit def tupleIterableIntToObserved(t: (Val[Array[Int]], Iterable[Int])): Observed[Array[Int]] = Observed(t._1, t._2.toArray)
      implicit def tupleIterableDoubleToObserved(t: (Val[Array[Double]], Iterable[Double])): Observed [Array[Double]]= Observed(t._1, t._2.toArray)
      implicit def tupleIterableArrayIntToObserved(t: (Val[Array[Int]], Array[Int])): Observed[Array[Int]] = Observed(t._1, t._2)
      implicit def tupleIterableArrayDoubleToObserved(t: (Val[Array[Double]], Array[Double])): Observed[Array[Double]] = Observed(t._1, t._2)
      //implicit def tupleToObserved[T: Observable](t: (Val[T], T)) = Observed(t._1, t._2)

      def fromContext[T](observed: Observed[T], context: Context) = context(observed.v.array).map(v => observed.obs(v))
      def value[T](observed: Observed[T]) = observed.obs(observed.observed)
    }

    case class Observed[T](v: Val[T], observed: T)(using val obs: Observed.Observable[T])

    case class ABCParameters(state: Val[MonAPMC.MonState], step: Val[Int], prior: IndependentPriors)

    implicit def method: ExplorationMethod[ABC, ABCParameters] = m => {
      implicit def defScope: DefinitionScope = m.scope
      val stepState = Val[MonAPMC.StepState]("stepState", abcNamespace)
      val step = Val[Int]("step", abcNamespace)

      val stop = Val[Boolean]

      val n = m.sample + m.generated
      val nAlpha = m.sample
      val priorValue = IndependentPriors(m.prior)

      val preStepTask = PreStepTask(n, nAlpha, priorValue, state, stepState, step, m.seed)
      val postStepTask = PostStepTask(n, nAlpha, m.stopSampleSizeFactor, priorValue, m.observed, state, stepState, m.minAcceptedRatio, m.maxStep, stop, step)

      val mapReduce =
        MapReduce(
          sampler = preStepTask,
          evaluation = m.evaluation,
          aggregation = postStepTask
        )

      val loop =
        While(
          evaluation = mapReduce,
          condition = !(stop: Condition)
        )

      DSLContainer(loop, output = Some(postStepTask), method = ABCParameters(state, step, priorValue))
    }

  }

  case class ABC(
    evaluation:           DSL,
    prior:                Seq[UnivariatePrior],
    observed:             Seq[ABC.Observed[_]],
    sample:               Int,
    generated:            Int,
    minAcceptedRatio:     OptionalArgument[Double] = 0.01,
    stopSampleSizeFactor: Int                      = 1,
    maxStep:              OptionalArgument[Int]    = None,
    seed:                 SeedVariable             = None,
    scope:                DefinitionScope          = "abc")

  object IslandABC {
    implicit def method: ExplorationMethod[IslandABC, ABC.ABCParameters] = m => {
      implicit def defScope: DefinitionScope = m.scope

      val masterState = Val[MonAPMC.MonState]("masterState", ABC.abcNamespace)
      val islandState = ABC.state

      val step = Val[Int]("masterStep", ABC.abcNamespace)
      val stop = Val[Boolean]

      val n = m.sample + m.generated
      val nAlpha = m.sample

      val priorValue = IndependentPriors(m.prior)

      val appendSplit = AppendSplitTask(n, nAlpha, masterState, islandState, step)
      val terminationTask =
        IslandTerminationTask(n, nAlpha, m.minAcceptedRatio, m.stopSampleSizeFactor, masterState, step, m.maxStep, stop) set (
          (inputs, outputs) += islandState.array
        )

      val master =
        MoleTask(appendSplit -- terminationTask) set (
          exploredOutputs += islandState.array,
          step := 0
        )

      val slave =
        MoleTask(
          ABC(
            evaluation = m.evaluation,
            prior = m.prior,
            observed = m.observed,
            sample = m.sample,
            generated = m.generated,
            minAcceptedRatio = m.minAcceptedRatio,
            maxStep = m.islandSteps,
            stopSampleSizeFactor = m.stopSampleSizeFactor,
            seed = m.seed
          )
        )

      val masterSlave =
        MasterSlave(
          SplitTask(masterState, islandState, m.parallelism),
          master = master,
          slave = slave,
          state = Seq(masterState, step),
          slaves = m.parallelism,
          stop = stop
        )

      DSLContainer(masterSlave, output = Some(master), delegate = Vector(slave), method = ABC.ABCParameters(masterState, step, priorValue))
    }
  }

  case class IslandABC(
    evaluation:           DSL,
    prior:                Seq[UnivariatePrior],
    observed:             Seq[ABC.Observed[_]],
    sample:               Int,
    generated:            Int,
    parallelism:          Int,
    minAcceptedRatio:     Double                = 0.01,
    stopSampleSizeFactor: Int                   = 1,
    maxStep:              OptionalArgument[Int] = None,
    islandSteps:          Int                   = 1,
    seed:                 SeedVariable          = None,
    scope:                DefinitionScope       = "abc island"
  )

  implicit class ABCContainer[M](m: M)(implicit method: ExplorationMethod[M, ABC.ABCParameters]) {
    val decorator = new MethodHookDecorator(m)(using method)
    export decorator.*
    
    def hook(directory: FromContext[File], frequency: Long = 1): Hooked[M] = {
      val dsl = method(m)
      implicit val defScope = dsl.scope
      Hooked(m, ABCHook(dsl.method, directory, frequency))
    }
  }

}
