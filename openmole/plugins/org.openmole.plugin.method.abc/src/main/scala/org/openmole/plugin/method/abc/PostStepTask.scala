package org.openmole.plugin.method.abc

import mgo.abc.{ MonAPMC, APMC }

import org.openmole.core.context.Variable
import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.core.setter.DefinitionScope
import org.openmole.core.workflow.task.FromContextTask
import util._

object PostStepTask {

  def apply(
    n:                    Int,
    nAlpha:               Int,
    stopSampleSizeFactor: Int,
    prior:                IndependentPriors,
    observed:             Seq[ABC.Observed[_]],
    state:                Val[MonAPMC.MonState],
    stepState:            Val[MonAPMC.StepState],
    minAcceptedRatio:     Option[Double],
    termination:          OptionalArgument[Int],
    stop:                 Val[Boolean],
    step:                 Val[Int])(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    FromContextTask("postStepTask") { p =>
      import p._

      def zipObserved(obs: Array[Array[Array[Double]]]) = {
        def zip2(o1: Array[Array[Double]], o2: Array[Array[Double]]) =
          for {
            l1 ← o1
            l2 ← o2
          } yield l1 ++ l2

        obs.reduceLeft { zip2 }
      }

      val xs = zipObserved(observed.toArray.map(o => ABC.Observed.fromContext(o, context)))

      val s = Try(MonAPMC.postStep(n, nAlpha, prior.density(p), observed.flatMap(o => ABC.Observed.value(o)).toArray, context(stepState), xs)(random()))

      s match {
        case Success(s) =>
          def stopValue(s: MonAPMC.MonState) =
            minAcceptedRatio.map(ar => MonAPMC.stop(n, nAlpha, ar, stopSampleSizeFactor, s)).getOrElse(false) ||
              termination.option.map(_ <= context(step)).getOrElse(false)

          context + Variable(state, s) + Variable(stop, stopValue(s)) + Variable(step, context(step) + 1)

        case Failure(f: APMC.SingularCovarianceException) =>
          def copyState(s: Option[MonAPMC.MonState], ns: APMC.State) =
            s match {
              case Some(MonAPMC.Empty()) | None => MonAPMC.Empty()
              case Some(s: MonAPMC.State)       => s.copy(s = ns)
            }

          context + Variable(state, context.getOrElse(state, MonAPMC.Empty())) + Variable(stop, true) + Variable(step, context(step) + 1)
        case Failure(f) => throw f
      }
    } set (
      inputs += stepState,
      inputs ++= observed.map(_.v.array),
      outputs += (state, stop),
      (inputs, outputs) += step
    )
}
