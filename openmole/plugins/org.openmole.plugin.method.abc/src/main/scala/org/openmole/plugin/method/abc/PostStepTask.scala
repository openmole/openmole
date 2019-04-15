package org.openmole.plugin.method.abc

import mgo.abc.MonAPMC

import org.openmole.core.context.Variable
import org.openmole.core.dsl.{ OptionalArgument, _ }
import org.openmole.core.workflow.builder.DefinitionScope
import org.openmole.core.workflow.task.FromContextTask

object PostStepTask {

  def apply(
    n:                Int,
    nAlpha:           Int,
    prior:            Seq[ABC.Prior],
    observed:         Seq[ABC.Observed],
    state:            Val[MonAPMC.MonState],
    stepState:        Val[MonAPMC.StepState],
    minAcceptedRatio: Option[Double],
    termination:      OptionalArgument[Int],
    stop:             Val[Boolean],
    step:             Val[Int])(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    FromContextTask("postStepTask") { p ⇒
      import p._

      val priorBounds = prior.map(pr ⇒ (pr.low.from(context), pr.high.from(context)))
      val volume = priorBounds.map { case (min, max) ⇒ math.abs(max - min) }.reduceLeft(_ * _)

      def density(point: Array[Double]) = {
        val inside = (priorBounds zip point).forall { case ((min, max), p) ⇒ p >= min && p <= max }
        if (inside) 1.0 / volume else 0.0
      }

      val xs = observed.toArray.map(o ⇒ context(o.v.array)).transpose
      val s = MonAPMC.postStep(n, nAlpha, density, observed.map(_.observed).toArray, context(stepState), xs)(random())
      val stopValue =
        minAcceptedRatio.map(ar ⇒ MonAPMC.stop(ar, s)).getOrElse(false) ||
          termination.option.map(_ <= context(step)).getOrElse(false)

      context + Variable(state, s) + Variable(stop, stopValue) + Variable(step, context(step) + 1)
    } set (
      inputs += stepState,
      inputs += (observed.map(_.v.array): _*),
      outputs += (state, stop),
      (inputs, outputs) += step
    )
}
