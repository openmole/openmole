package org.openmole.plugin.method.abc

import mgo.abc.MonAPMC
import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.workflow.task.FromContextTask

object SplitTask {

  def apply(masterState: Val[MonAPMC.MonState], islandState: Val[MonAPMC.MonState], n: Int)(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    FromContextTask("splitTask") { p =>
      import p._

      def generateStates(s: MonAPMC.MonState, acc: List[MonAPMC.MonState], cpt: Int): (MonAPMC.MonState, Array[MonAPMC.MonState]) = {
        val (s1, s2) = MonAPMC.split(s)
        if(cpt >= n) (s1, acc.toArray)
        else generateStates(s1, s2 :: acc, cpt + 1)
      }

      val (ms, states) = generateStates(context(masterState), List(), 0)

      context + (islandState.array -> states) + (masterState -> ms)
    } set (
      (inputs, outputs) += masterState,
      exploredOutputs += islandState.array,
      masterState := MonAPMC.Empty(),
    )

}
