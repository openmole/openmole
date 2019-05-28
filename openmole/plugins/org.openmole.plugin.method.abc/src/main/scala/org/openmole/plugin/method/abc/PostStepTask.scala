package org.openmole.plugin.method.abc

import mgo.abc.{ MonAPMC, APMC }

import org.openmole.core.context.Variable
import org.openmole.core.dsl.{ OptionalArgument, _ }
import org.openmole.core.workflow.builder.DefinitionScope
import org.openmole.core.workflow.task.FromContextTask
import util._

object PostStepTask {

  def apply(
    n:                    Int,
    nAlpha:               Int,
    stopSampleSizeFactor: Int,
    prior:                Seq[ABC.Prior],
    observed:             Seq[ABC.Observed[_]],
    state:                Val[MonAPMC.MonState],
    stepState:            Val[MonAPMC.StepState],
    minAcceptedRatio:     Option[Double],
    termination:          OptionalArgument[Int],
    stop:                 Val[Boolean],
    step:                 Val[Int])(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    FromContextTask("postStepTask") { p ⇒
      import p._

      val priorBounds = prior.map(pr ⇒ (pr.low.from(context), pr.high.from(context)))
      val volume = priorBounds.map { case (min, max) ⇒ math.abs(max - min) }.reduceLeft(_ * _)

      def density(point: Array[Double]) = {
        val inside = (priorBounds zip point).forall { case ((min, max), p) ⇒ p >= min && p <= max }
        if (inside) 1.0 / volume else 0.0
      }

      val xs = observed.toArray.map(o ⇒ ABC.Observed.fromContext(o, context).flatten).transpose

      val s = Try(MonAPMC.postStep(n, nAlpha, density, observed.flatMap(o ⇒ ABC.Observed.value(o)).toArray, context(stepState), xs)(random()))

      s match {
        case Success(s) ⇒
          def stopValue(s: MonAPMC.MonState) =
            minAcceptedRatio.map(ar ⇒ MonAPMC.stop(n, nAlpha, ar, stopSampleSizeFactor, s)).getOrElse(false) ||
              termination.option.map(_ <= context(step)).getOrElse(false)

          context + Variable(state, s) + Variable(stop, stopValue(s)) + Variable(step, context(step) + 1)

        case Failure(f: APMC.SingularCovarianceException) ⇒
          def copyState(s: Option[MonAPMC.MonState], ns: APMC.State) =
            s match {
              case Some(MonAPMC.Empty()) | None ⇒ MonAPMC.Empty()
              case Some(s: MonAPMC.State)       ⇒ s.copy(s = ns)
            }

          context + Variable(state, copyState(context.get(state), f.s)) + Variable(stop, true) + Variable(step, context(step) + 1)
        case Failure(f) ⇒ throw f
      }
    } set (
      inputs += stepState,
      inputs += (observed.map(_.v.array): _*),
      outputs += (state, stop),
      (inputs, outputs) += step
    )
}
