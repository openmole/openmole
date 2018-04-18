package org.openmole.plugin.method.evolution

import org.openmole.core.dsl._
import org.openmole.core.workflow.task.ClosureTask
import org.openmole.core.context.Variable
import org.openmole.core.workflow.builder.DefinitionScope
import org.openmole.core.workflow.puzzle.Puzzle

object DeltaTask {
  def apply(objective: (Val[Double], Double)*)(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    ClosureTask("DeltaTask") { (context, _, _) ⇒
      context ++ objective.map { case (v, o) ⇒ Variable(v, math.abs(context(v) - o)) }
    } set (
      (inputs, outputs) += (objective.map(_._1): _*)
    )
}

object Delta {
  import org.openmole.core.workflow.builder.DefinitionScope
  implicit def scope = DefinitionScope.Internal
  
  def apply(puzzle: Puzzle, objective: (Val[Double], Double)*) =
    puzzle -- DeltaTask(objective: _*)

}
