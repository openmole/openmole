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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.implementation.mole

import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.core.model.mole.IMole
import org.openmole.core.model.task.IGenericTask
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer

class Mole(val root: IGenericCapsule) extends IMole {

  @throws(classOf[Throwable])
  override def tasks: Iterable[IGenericTask] = {
    val tasks = new HashSet[IGenericTask]

    capsules.foreach(visited => { 
        visited.task match {
          case None =>
          case Some(task) => tasks += task
        }
            
      })

    tasks
  }

  @throws(classOf[Throwable])
  override def capsules: Iterable[IGenericCapsule] = {
    val caps = new HashSet[IGenericCapsule]
    val toExplore = new ListBuffer[IGenericCapsule]
    toExplore += root

    while (!(toExplore.isEmpty)) {
      val current = toExplore.remove(0)

      if (!caps.contains(current)) {
        for (transition <- current.outputTransitions) {
          toExplore += transition.end.capsule
        }
        caps += current
      }
    }
    caps
  }
}
