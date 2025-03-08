package org.openmole.plugin.tool.pattern

import org.openmole.core.dsl._

object Sequence {
  def apply(p1: DSL, puzzles: DSL*) = puzzles.foldLeft(p1)((p1, p2) => p1 -- p2)
}
