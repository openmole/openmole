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

import java.util.logging.Logger
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.eventdispatcher.IObjectListenerWithArgs
import org.openmole.misc.exception.{InternalProcessingError, UserBadDataError}
import org.openmole.misc.tools.service.Priority
import org.openmole.core.implementation.tools.ContextBuffer
import org.openmole.core.model.capsule.ICapsule
import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.core.model.data.IContext
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.job.ITicket
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.mole.ISubMoleExecution
import org.openmole.core.model.transition.IAggregationTransition
import org.openmole.core.model.transition.ICondition
import org.openmole.core.model.transition.ISlot

class AggregationTransition(start: ICapsule, end: ISlot, condition: ICondition, filtered: Set[String]) extends Transition(start, end, condition, filtered) with IAggregationTransition {

  class AggregationTransitionAdapter extends IObjectListenerWithArgs[ISubMoleExecution] {

    override def eventOccured(subMole: ISubMoleExecution, args: Array[Object]) = {
      val lastJob = args(0).asInstanceOf[IMoleJob]
      val moleExecution = args(1).asInstanceOf[IMoleExecution]
      val ticket = args(2).asInstanceOf[ITicket];
      subMoleFinished(subMole, lastJob, ticket, moleExecution)
    }
  }

  def this(start: ICapsule, end: IGenericCapsule) = this(start, end.defaultInputSlot, ICondition.True, Set.empty[String])
    
  def this(start: ICapsule, end: IGenericCapsule, condition: ICondition) = this(start, end.defaultInputSlot, condition, Set.empty[String])

  def this(start: ICapsule, end: IGenericCapsule, condition: String) = this(start, end.defaultInputSlot, new Condition(condition), Set.empty[String])
    
  def this(start: ICapsule , slot: ISlot, condition: String) = this(start, slot, new Condition(condition), Set.empty[String])
    
  def this(start: ICapsule , slot: ISlot, condition: ICondition) = this(start, slot, condition, Set.empty[String])
   
  def this(start: ICapsule, end: IGenericCapsule, filtred: Array[String]) = this(start, end.defaultInputSlot, ICondition.True, filtred.toSet)
    
  def this(start: ICapsule, end: IGenericCapsule, condition: ICondition, filtred: Array[String]) = this(start, end.defaultInputSlot, condition, filtred.toSet)

  def this(start: ICapsule, end: IGenericCapsule, condition: String, filtred: Array[String]) = this(start, end.defaultInputSlot, new Condition(condition), filtred.toSet)
    
  def this(start: ICapsule , slot: ISlot, condition: String, filtred: Array[String]) = this(start, slot, new Condition(condition), filtred.toSet)

  override def performImpl(context: IContext, ticket: ITicket, toClone: Set[String], moleExecution: IMoleExecution, subMole: ISubMoleExecution) = synchronized {

    val registry = moleExecution.localCommunication.aggregationTransitionRegistry

    val parent = ticket.parent.getOrElse(throw new UserBadDataError("Aggregation transition should take place after an exploration"))
    
    val resultContexts = registry.consult(this, parent) match {
      case None => 
        val res = new ContextBuffer
        registry.register(this, parent, res)
        EventDispatcher.registerForObjectChangedSynchronous(subMole, Priority.LOW, new AggregationTransitionAdapter, ISubMoleExecution.Finished)
        res
      case Some(res) => res
    }
 
    //Store the result context
    resultContexts ++= (context, toClone)
  }

  def subMoleFinished(subMole: ISubMoleExecution, job: IMoleJob, ticket: ITicket, moleExecution: IMoleExecution) = {
    def registry =  moleExecution.localCommunication.aggregationTransitionRegistry

    val newTicket = ticket.parent.getOrElse(throw new UserBadDataError("Aggregation transition should take place after an exploration"))
    val result = registry.remove(this, newTicket).getOrElse(throw new InternalProcessingError("No context registred for the aggregation transition"))
    val endTask = end.capsule.task.getOrElse(throw new UserBadDataError("No task assigned for end capsule"))
    val startTask = start.task.getOrElse(throw new UserBadDataError("No task assigned for start capsule"))
    val subMoleParent = subMole.parent.getOrElse(throw new InternalProcessingError("Submole execution has no parent"))

    //Variable have are clonned in other transitions if necessary
    submitNextJobsIfReady(result, newTicket, moleExecution, subMoleParent)
  }
}
