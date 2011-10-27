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

import org.openmole.core.model.data.IContext
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.mole.ISubMoleExecution
import org.openmole.core.model.mole.ITicket
import org.openmole.core.model.transition.ICondition
import org.openmole.core.model.transition.ISlot
import org.openmole.misc.exception.UserBadDataError

class SlaveTransition(start: ICapsule, end: ISlot, condition: ICondition, filtered: Set[String]) extends ExplorationTransition(start, end, condition, filtered) {

  def this(start: ICapsule, end: ICapsule) = this(start, new Slot(end), ICondition.True, Set.empty[String])
    
  def this(start: ICapsule, end: ISlot) = this(start, end, ICondition.True, Set.empty[String])
  
  def this(start: ICapsule, end: ICapsule, condition: ICondition) = this(start, new Slot(end), condition, Set.empty[String])

  def this(start: ICapsule, end: ICapsule, condition: String) = this(start, new Slot(end), new Condition(condition), Set.empty[String])

  def this(start: ICapsule, slot: ISlot, condition: String) = this(start, slot, new Condition(condition), Set.empty[String])

  def this(start: ICapsule, slot: ISlot, condition: ICondition) = this(start, slot, condition, Set.empty[String])

  def this(start: ICapsule, end: ICapsule, filtred: Array[String]) = this(start, new Slot(end), ICondition.True, filtred.toSet)

  def this(start: ICapsule, end: ICapsule, condition: ICondition, filtred: Array[String]) = this(start, new Slot(end), condition, filtred.toSet)

  def this(start: ICapsule, end: ICapsule, condition: String, filtred: Array[String]) = this(start, new Slot(end), new Condition(condition), filtred.toSet)

  def this(start: ICapsule , slot: ISlot, condition: String, filtred: Array[String]) = this(start, slot, new Condition(condition), filtred.toSet)
  
  override def _perform(context: IContext, ticket: ITicket, subMole: ISubMoleExecution) = submitIn(context, ticket.parent.getOrElse(throw new UserBadDataError("Slave transition should take place after an master transition.")), subMole)
  

}
