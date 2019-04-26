package org.openmole.plugin.method.abc

import mgo.abc.MonAPMC
import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.workflow.task.FromContextTask

object IslandTerminationTask {

  def apply(
    n:                    Int,
    nAlpha:               Int,
    minAcceptedRatio:     Double,
    stopSampleSizeFactor: Int,
    state:                Val[MonAPMC.MonState],
    step:                 Val[Int],
    maxStep:              OptionalArgument[Int],
    stop:                 Val[Boolean])(
    implicit
    name:            sourcecode.Name,
    definitionScope: DefinitionScope) =
    FromContextTask("appendTask") { p ⇒
      import p._

      context(state) match {
        case MonAPMC.State(t0, s) ⇒
          val tSpan = ((stopSampleSizeFactor * nAlpha).toDouble /
            (n - nAlpha).toDouble).ceil
          val count = s.ts.count { _ > s.t - tSpan }
          val pAcc = count.toDouble / (tSpan * (n - nAlpha)).toDouble
          //(s.t >= tSpan) && (pAccMin >= pAcc)

          println("n " ++ n.toString)
          println("nAlpha " ++ nAlpha.toString)
          println("minAcceptedRatio " ++ minAcceptedRatio.toString)
          println("tSpan " ++ tSpan.toString)
          println("s.t " ++ s.t.toString)
          println("pAcc " ++ pAcc.toString)
          println("s.ts " ++ s.ts.mkString(","))
          println("s.epsilon " ++ s.epsilon.toString)
      }

      val stopValue =
        maxStep.option.map(ms ⇒ context(step) >= ms).getOrElse(false) ||
          MonAPMC.stop(n, nAlpha, minAcceptedRatio, stopSampleSizeFactor, context(state))

      context + (stop -> stopValue)
    } set (
      (inputs, outputs) += (state, step),
      outputs += stop
    )

}
