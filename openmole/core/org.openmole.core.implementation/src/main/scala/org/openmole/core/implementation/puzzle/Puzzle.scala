/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.implementation.puzzle

import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.mole.IEnvironmentSelection
import org.openmole.core.model.mole.IGrouping
import org.openmole.core.model.transition.ISlot

case class Puzzle(
  val first: ISlot,
  val last: ICapsule,
  val selection: Map[ICapsule, IEnvironmentSelection],
  val grouping: Map[ICapsule, IGrouping]
) {
  
  def +(p: Puzzle) = 
    new Puzzle(
      first,
      p.last,
      selection ++ p.selection,
      grouping ++ p.grouping
    )
  
}