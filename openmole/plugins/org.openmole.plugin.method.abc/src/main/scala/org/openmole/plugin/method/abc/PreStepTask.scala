package org.openmole.plugin.method.abc

import org.openmole.core.workflow.task.FromContextTask
import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import mgo.abc._
import org.openmole.core.context.Variable
import org.openmole.core.argument.FromContext
import org.openmole.tool.math._
import org.openmole.core.setter.DefinitionScope

object PreStepTask {

  def apply(
    n:         Int,
    nAlpha:    Int,
    prior:     IndependentPriors,
    state:     Val[MonAPMC.MonState],
    stepState: Val[MonAPMC.StepState],
    step:      Val[Int],
    seed:      SeedVariable)(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    FromContextTask("preStepTask") { p =>
      import p._

      val s = context(state)
      val (ns, matrix: Array[Array[Double]]) =
        MonAPMC.preStep(n, nAlpha, x => prior.sample(x).from(context), x => prior.density(x).from(context), s)(using random())

      val rng = random()
      val samples =
        (prior.v zip matrix.toVector.transpose).flatMap {
          case (v, samples) =>
            Seq(Variable(v.array, samples.toArray)) ++ seed.array(samples.size, rng).toSeq
        }

      context ++ samples + Variable(stepState, ns)
    } set (
      (inputs, outputs) += step,
      inputs += state,
      exploredOutputs ++= prior.v.map(_.array) ++ seed.prototype.map(_.array).toSeq,
      outputs += stepState,

      state := MonAPMC.Empty(),
      step := 0
    )

}

