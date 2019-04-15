package org.openmole.plugin.method.abc

import mgo.abc.MonAPMC
import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.workflow.task.FromContextTask

object AppendSplitTask {

  def apply(n: Int, nAlpha: Int, state: Val[MonAPMC.MonState], islandState: Val[MonAPMC.MonState], step: Val[Int])(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    FromContextTask("appendTask") { p ⇒
      import p._
      val newState = MonAPMC.append(n, nAlpha, context(state), context(islandState))
      val (ns1, ns2) = MonAPMC.split(newState)
      context + (state.array -> Array(ns1)) + (islandState -> ns2) + (step -> (context(step) + 1))
    } set (
      (inputs, outputs) += (islandState, step),
      inputs += state,
      exploredOutputs += state.array
    )

}
