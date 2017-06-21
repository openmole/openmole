package org.openmole.plugin.tool.pattern

import org.openmole.core.dsl._
import org.openmole.core.workflow.puzzle._

object Sequence {
  def apply(p1: Puzzle, puzzles: Puzzle*) = puzzles.foldLeft(p1)((p1, p2) ⇒ p1 -- p2)
}
