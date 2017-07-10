package org.openmole.plugin.tool.pattern

import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._
import org.openmole.core.context._
import org.openmole.core.expansion._

object While {

  def apply(
    puzzle:    Puzzle,
    condition: Condition,
    counter:   OptionalArgument[Val[Long]] = None
  ): Puzzle =
    counter.option match {
      case None ⇒
        val last = Capsule(EmptyTask(), strain = true)
        (puzzle -- (last when !condition)) & (puzzle -- (Slot(puzzle.first) when condition))
      case Some(counter) ⇒
        val firstTask = EmptyTask() set (
          (inputs, outputs) += counter,
          counter := 0L,
          (inputs, outputs) += (puzzle.inputs: _*),
          defaults += (puzzle.defaults: _*)
        )

        val first = Capsule(firstTask)

        val incrementTask =
          ClosureTask("IncrementTask") { (ctx, _, _) ⇒
            ctx + (counter → (ctx(counter) + 1))
          } set (
            (inputs, outputs) += counter,
            (inputs, outputs) += (puzzle.outputs: _*)
          )

        val last = EmptyTask() set (
          (inputs, outputs) += counter,
          (inputs, outputs) += (puzzle.outputs: _*)
        )

        (first -- puzzle -- incrementTask -- (last when !condition)) &
          (puzzle -- incrementTask -- (first when condition))
    }

}
