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

package org.openmole.core.implementation.mole

import org.openmole.core.implementation.data.Context
import org.openmole.core.model.data.IContext
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.mole.IMole
import org.openmole.core.model.task.ITask
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer

class Mole(val root: ICapsule, val implicits: IContext = Context.empty) extends IMole {

  override def tasks: Iterable[ITask] = capsules.flatMap(_.task).toSet

  override def capsules: Seq[ICapsule] = {
    val visited = new HashSet[ICapsule]
    val list = new ListBuffer[ICapsule]
    val toExplore = new ListBuffer[ICapsule]

    toExplore += root

    while (!(toExplore.isEmpty)) {
      val current = toExplore.remove(0)

      if (!visited.contains(current)) {
        for (transition ← current.outputTransitions) {
          toExplore += transition.end.capsule
        }
        visited += current
        list += current
      }
    }
    list
  }

}
