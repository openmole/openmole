package org.openmole.plugin.method.abc

import mgo.abc.MonAPMC
import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.workflow.task.FromContextTask

object IslandTerminationTask {

  def apply(minAcceptedRatio: Double, state: Val[MonAPMC.MonState], step: Val[Int], maxStep: OptionalArgument[Int], stop: Val[Boolean])(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    FromContextTask("appendTask") { p ⇒
      import p._

      val stopValue =
        maxStep.option.map(ms ⇒ context(step) >= ms).getOrElse(false) ||
          MonAPMC.stop(minAcceptedRatio, context(state))

      context + (stop -> stopValue)
    } set (
      inputs += (state, step),
      outputs += stop
    )

}
