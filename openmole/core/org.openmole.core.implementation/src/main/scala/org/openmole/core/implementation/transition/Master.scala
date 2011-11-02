/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.core.implementation.transition

import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.task.ITask
import org.openmole.core.model.transition.IMaster
import org.openmole.core.model.transition.ICondition
import org.openmole.core.model.transition.ICondition._
import org.openmole.core.model.transition.ISlot
import org.openmole.core.implementation.mole.Capsule

class Master(val selection: ITask, val master: ISlot, val condition: ICondition) extends IMaster {
  def this(selection: ITask, master: ISlot) = this(selection, master, True)
  def this(selection: ITask, master: ICapsule) = this(selection, master.defaultInputSlot, True)
  def this(selection: ITask, master: ICapsule, condition: ICondition) = this(selection, master.defaultInputSlot, condition)
  
  val transition = new Transition(new Capsule(selection), master, condition)
}
