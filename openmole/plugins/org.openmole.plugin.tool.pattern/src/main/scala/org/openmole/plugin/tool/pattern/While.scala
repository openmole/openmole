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
        (puzzle -- (last, !condition)) & (puzzle -- (Slot(puzzle.first), condition))
      case Some(counter) ⇒
        val incrementTask =
          ClosureTask("IncrementTask") { (ctx, _, _) ⇒
            ctx + (counter → (ctx(counter) + 1))
          } set (
            (inputs, outputs) += counter,
            counter := 0L
          )

        val increment = MasterCapsule(incrementTask, persist = Seq(counter), strain = true)
        val last = Capsule(EmptyTask(), strain = true)

        (puzzle -- increment -- (last, !condition)) & (increment -- (Slot(puzzle.first), condition))
    }

}
