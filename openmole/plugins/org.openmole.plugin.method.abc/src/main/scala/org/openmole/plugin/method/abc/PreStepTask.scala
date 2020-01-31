package org.openmole.plugin.method.abc

import org.openmole.core.workflow.task.FromContextTask
import org.openmole.core.dsl._
import mgo.abc._
import org.openmole.core.context.Variable
import org.openmole.core.expansion.FromContext
import org.openmole.core.tools.math._
import org.openmole.core.workflow.builder.DefinitionScope

import scala.util.Random

object PreStepTask {

  def apply(n: Int, nAlpha: Int, prior: IndependentPriors, state: Val[MonAPMC.MonState], stepState: Val[MonAPMC.StepState], step: Val[Int])(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    FromContextTask("preStepTask") { p ⇒
      import p._

      val s = context(state)
      val (ns, matrix: Array[Array[Double]]) =
        MonAPMC.preStep(n, nAlpha, prior.sample(p)(_), prior.density(p)(_), s)(random())

      val samples = (prior.v zip matrix.toVector.transpose).map { case (v, samples) ⇒ Variable(v.array, samples.toArray) }

      context ++ samples + Variable(stepState, ns)
    } set (
      (inputs, outputs) += step,
      inputs += state,
      exploredOutputs ++= prior.v.map(_.array),
      outputs += stepState,

      state := MonAPMC.Empty(),
      step := 0
    )

}

