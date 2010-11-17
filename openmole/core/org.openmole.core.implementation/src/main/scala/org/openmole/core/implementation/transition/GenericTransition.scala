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
import org.openmole.core.implementation.data.Context
import org.openmole.core.implementation.tools.ContextAggregator
import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.core.model.data.IContext
import org.openmole.core.model.job.ITicket
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.mole.ISubMoleExecution
import org.openmole.core.model.task.IGenericTask
import org.openmole.core.model.transition.ICondition
import org.openmole.core.model.transition.IGenericTransition
import org.openmole.core.model.transition.ISlot
import scala.collection.mutable.ListBuffer

abstract class GenericTransition(val start: IGenericCapsule, val end: ISlot, val condition: ICondition, val filtered: Set[String]) extends IGenericTransition {

    /*def this(start: TS, end: IGenericTaskCapsule[_,_]) = this(start, end.defaultInputSlot, None, Set.empty)
    
    def this(start: TS, end: IGenericTaskCapsule[_,_], condition: ICondition) = this(start, end.getDefaultInputSlot(), Some(condition), Set.empty)

    def this(start: TS, end: IGenericTaskCapsule[_,_], condition: String) = this(start, end.getDefaultInputSlot(), new Condition(condition), Set.empty)
    
    def this(start: TS , slot: ISlot, condition: String) = this(start, slot, new Condition(condition), Set.empty)
    
    def this(start: TS , slot: ISlot, condition: ICondition) = this(start, slot, condition, Set.empty)
   
    def this(start: TS, end: IGenericTaskCapsule[_,_], filtred: String*) = this(start, end.defaultInputSlot, None, filtred)
    
    def this(start: TS, end: IGenericTaskCapsule[_,_], condition: ICondition, filtred: String*) = this(start, end.getDefaultInputSlot(), Some(condition), filtred)

    def this(start: TS, end: IGenericTaskCapsule[_,_], condition: String, filtred: String*) = this(start, end.getDefaultInputSlot(), new Condition(condition), filtred)
    
    def this(start: TS , slot: ISlot, condition: String, filtred: String*) = this(start, slot, new Condition(condition), filtred)
    
    def this(start: TS , slot: ISlot, condition: ICondition, filtred: String*) = this(start, slot, condition, filtred)
*/
    plugStart 
    end.plugTransition(this)
  

    def nextTaskReady(context: IContext, ticket: ITicket, execution: IMoleExecution): Boolean = {
        val registry = execution.localCommunication.transitionRegistry

        for (t <- end.transitions) {
            if (!registry.isRegistred(t, ticket)) return false
        }
        return true
    }

    protected def submitNextJobsIfReady(global: IContext, context: IContext, ticket: ITicket, toClone: Set[String], moleExecution: IMoleExecution, subMole: ISubMoleExecution) = synchronized {
             
       

        var allVarToClone = toClone 
        val registry = moleExecution.localCommunication.transitionRegistry
        registry.register(this, ticket, context)

        if (nextTaskReady(context, ticket, moleExecution)) {
            val combinaison = new ListBuffer[IContext]

            for (t <- end.transitions) combinaison += {
              (registry.remove(t, ticket) match {
                  case None => throw new InternalProcessingError("BUG Context not registred for transtion")
                  case Some(c) => c
              })
            }
            
            val itDc = end.capsule.inputDataChannels
            for (dataChannel <- itDc) {
              
                val res = dataChannel.consums(context, ticket, moleExecution)
                allVarToClone ++= res._2
                combinaison += res._1
            }

            val newTicket =  if (end.capsule.intputSlots.size <= 1)  ticket else {
              moleExecution.nextTicket(ticket.parent match {
                  case None => throw new InternalProcessingError("BUG should never reach root ticket")
                  case Some(t) => t
              })
            } 

            //Agregate the variables = inputs for the next job
            val newContextEnd = new Context

            val endTask = end.capsule.task match {
              case None => throw new InternalProcessingError("End task capsule of the transition is no assigned")
              case Some(t) => t
            }  
      
            ContextAggregator.aggregate(newContextEnd, endTask.inputs, toClone, false, combinaison)

           // Logger.getLogger(Transition.class.getName()).info("Submit job for task " + getEnd().getCapsule().getTask().getName());
            moleExecution.submit(end.capsule,  global, newContextEnd, newTicket, subMole)
        }
    }

    override def perform(global: IContext, context: IContext, ticket: ITicket, toClone: Set[String], scheduler: IMoleExecution, subMole: ISubMoleExecution) = {
        if (isConditionTrue(global, context)) {
            /*-- Remove filtred --*/
            for(name <- filtered) context -= name

            performImpl(global, context, ticket, toClone, scheduler, subMole);
        }
    }


    override def isConditionTrue(global: IContext, context: IContext): Boolean = {
      condition.evaluate(global, context)
    }

    protected def performImpl(global: IContext, context: IContext, ticket: ITicket, toClone: Set[String], scheduler: IMoleExecution, subMole: ISubMoleExecution) 
    protected def plugStart
}
