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

package org.openmole.core.implementation.validation

import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.transition.ITransition

object TopologyProblem {

  case class DuplicatedTransition(val transitions: Iterable[ITransition]) extends TopologyProblem {

    override def toString = "DuplicatedTransition: from " + transitions.head.start + " to " + transitions.head.end.capsule + " has been found " + transitions.size + " times."
  }

  case class LevelProblem(
      val capsule: ICapsule,
      val paths: List[(List[ICapsule], Int)]) extends TopologyProblem {

    override def toString = "LevelProblem: " + capsule + ", " + paths.map { case (p, l) ⇒ "Folowing the path (" + p.mkString(", ") + " has level " + l + ")" }.mkString(", ")
  }

  case class NegativeLevelProblem(
      val capsule: ICapsule,
      val path: List[ICapsule],
      val level: Int) extends TopologyProblem {

    override def toString = "LevelProblem: " + capsule + ", " + path.mkString(", ") + " has a negative level " + level
  }
}

trait TopologyProblem extends Problem