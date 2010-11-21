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

package org.openmole.core.implementation.transition

import org.openmole.commons.exception.InternalProcessingError
import org.openmole.commons.exception.UserBadDataError
import org.openmole.core.implementation.data.Context
import org.openmole.core.implementation.tools.ContextBuffer
import org.openmole.core.implementation.tools.LevelComputing
import org.openmole.core.model.capsule.ICapsule
import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.core.model.data.IContext
import org.openmole.core.model.job.ITicket
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.mole.ISubMoleExecution
import org.openmole.core.model.transition.ICondition
import org.openmole.core.model.transition.ISlot
import org.openmole.core.model.transition.ITransition

class Transition(override val start: ICapsule, override val end: ISlot, override val condition: ICondition, filtred: Set[String])  extends GenericTransition(start, end, condition, filtred) with ITransition {

  def this(start: ICapsule, end: IGenericCapsule) = this(start, end.defaultInputSlot, ICondition.True, Set.empty[String])
    
  def this(start: ICapsule, end: IGenericCapsule, condition: ICondition) = this(start, end.defaultInputSlot, condition, Set.empty[String])

  def this(start: ICapsule, end: IGenericCapsule, condition: String) = this(start, end.defaultInputSlot, new Condition(condition), Set.empty[String])
    
  def this(start: ICapsule , slot: ISlot, condition: String) = this(start, slot, new Condition(condition), Set.empty[String])
    
  def this(start: ICapsule , slot: ISlot, condition: ICondition) = this(start, slot, condition, Set.empty[String])
   
  def this(start: ICapsule, end: IGenericCapsule, filtred: Array[String]) = this(start, end.defaultInputSlot, ICondition.True, filtred.toSet)
    
  def this(start: ICapsule, end: IGenericCapsule, condition: ICondition, filtred: Array[String]) = this(start, end.defaultInputSlot, condition, filtred.toSet)

  def this(start: ICapsule, end: IGenericCapsule, condition: String, filtred: Array[String]) = this(start, end.defaultInputSlot, new Condition(condition), filtred.toSet)
    
  def this(start: ICapsule , slot: ISlot, condition: String, filtred: Array[String]) = this(start, slot, new Condition(condition), filtred.toSet)
    

  override def performImpl(global: IContext, context: IContext, ticket: ITicket, toClone: Set[String], moleExecution: IMoleExecution, subMole: ISubMoleExecution) = {
    val level = LevelComputing(moleExecution)

    val beginLevel = level.level(start)
    val endLevel = level.level(end.capsule)

    if(endLevel > beginLevel) {
      throw new UserBadDataError("The transition going from " + start.task.toString + " to " + end.capsule.task.toString + " doesn't match the secifications.");
    }

    var destTicket = ticket
    var newSubMole = subMole

    for(i <- beginLevel until endLevel) {
      destTicket = ticket.parent match {
        case None => throw new InternalProcessingError("BUG Should never reach root ticket")
        case Some(t) => t
      }
      newSubMole = newSubMole.parent match {
        case None => throw new InternalProcessingError("BUG Should never reach root submole")
        case Some(sm) => sm
      } 
    }

    end.synchronized  {
      submitNextJobsIfReady(global, ContextBuffer(context, toClone), destTicket, moleExecution, newSubMole);
    }
  }
  
  override protected def plugStart = start.plugOutputTransition(this)
  
}
