/*
 * Copyright (C) 2012 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.workflow.validation

import org.openmole.core.workflow.mole.MoleCapsule
import org.openmole.core.workflow.task.MoleTask
import org.openmole.core.workflow.transition.{ DataChannel, Transition }

/**
 * Problems with the validation of Puzzle's topology
 */
object TopologyProblem {

  case class DuplicatedTransition(transitions: Iterable[Transition]) extends TopologyProblem {
    override def toString = "DuplicatedTransition: from " + transitions.head.start + " to " + transitions.head.end.capsule + " has been found " + transitions.size + " times."
  }

  case class LevelProblem(
    capsule: MoleCapsule,
    paths:   List[(List[MoleCapsule], Int)]
  ) extends TopologyProblem {
    override def toString = "LevelProblem: " + capsule + ", " + paths.map { case (p, l) => "Following the path (" + p.mkString(", ") + " has level " + l + ")" }.mkString(", ")
  }

  case class NegativeLevelProblem(
    capsule: MoleCapsule,
    path:    List[MoleCapsule],
    level:   Int
  ) extends TopologyProblem {

    override def toString = "LevelProblem: " + capsule + " has a negative level " + level + " following the path " + path.mkString(", ")
  }

  case class DataChannelNegativeLevelProblem(dataChannel: DataChannel) extends TopologyProblem {
    override def toString = "DataChannelNegativeLevelProblem: " + dataChannel + ", links a capsule of upper level to lower level, this is not supported, use aggregation transitions."
  }

  case class NoTransitionToCapsuleProblem(capsule: MoleCapsule, dataChannel: DataChannel) extends TopologyProblem {
    override def toString = s"NoTransitionToCapsuleProblem: $capsule is linked with $dataChannel but not with any transition"
  }

  case class UnreachableCapsuleProblem(capsule: MoleCapsule) extends TopologyProblem {
    override def toString = s"UnreachableCapsuleProblem: $capsule is not linked to the workflow by any transition"
  }

  case class MoleTaskLastCapsuleProblem(capsule: MoleCapsule, moleTask: MoleTask, level: Int) extends TopologyProblem {
    override def toString = s"MoleTaskLastCapsuleProblem: in mole task $capsule the last capsule ${moleTask.last} has been found at level $level, it should be at level 0"
  }
}

trait TopologyProblem extends Problem