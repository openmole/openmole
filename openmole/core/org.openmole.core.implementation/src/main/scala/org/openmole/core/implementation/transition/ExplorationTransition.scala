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
import org.openmole.misc.exception.{InternalProcessingError,UserBadDataError}
import org.openmole.core.implementation.data.Context
import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.mole.SubMoleExecution
import org.openmole.core.implementation.task.ExplorationTask
import org.openmole.core.implementation.tools.ContextBuffer
import org.openmole.core.model.capsule.IExplorationCapsule
import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IData
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.mole.ITicket
import org.openmole.core.model.mole.ISubMoleExecution
import org.openmole.core.model.transition.IAggregationTransition
import org.openmole.core.model.transition.ICondition
import org.openmole.core.model.transition.IExplorationTransition
import org.openmole.core.model.transition.IGenericTransition
import org.openmole.core.model.transition.ISlot
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.tools.service.Priority
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer

class ExplorationTransition(override val start: IExplorationCapsule, override val end: ISlot, override val condition: ICondition, override val filtered: Set[String]) extends GenericTransition(start, end, condition, filtered) with IExplorationTransition {
 
  def this(start: IExplorationCapsule, end: IGenericCapsule) = this(start, end.defaultInputSlot, ICondition.True, Set.empty[String])
    
  def this(start: IExplorationCapsule, end: IGenericCapsule, condition: ICondition) = this(start, end.defaultInputSlot, condition, Set.empty[String])

  def this(start: IExplorationCapsule, end: IGenericCapsule, condition: String) = this(start, end.defaultInputSlot, new Condition(condition), Set.empty[String])
    
  def this(start: IExplorationCapsule, slot: ISlot, condition: String) = this(start, slot, new Condition(condition), Set.empty[String])
    
  def this(start: IExplorationCapsule, slot: ISlot, condition: ICondition) = this(start, slot, condition, Set.empty[String])
   
  def this(start: IExplorationCapsule, end: IGenericCapsule, filtred: Array[String]) = this(start, end.defaultInputSlot, ICondition.True, filtred.toSet)
    
  def this(start: IExplorationCapsule, end: IGenericCapsule, condition: ICondition, filtred: Array[String]) = this(start, end.defaultInputSlot, condition, filtred.toSet)

  def this(start: IExplorationCapsule, end: IGenericCapsule, condition: String, filtred: Array[String]) = this(start, end.defaultInputSlot, new Condition(condition), filtred.toSet)
    
  def this(start: IExplorationCapsule, slot: ISlot, condition: String, filtred: Array[String]) = this(start, slot, new Condition(condition), filtred.toSet)

  
  override def performImpl(context: IContext, ticket: ITicket, toClone: Set[String], subMole: ISubMoleExecution) = synchronized {
    import subMole.moleExecution
    val values = context.value(ExplorationTask.Sample.prototype).getOrElse(throw new InternalProcessingError("BUG Sample not found in the exploration transition"))
    val subSubMole = SubMoleExecution(moleExecution, subMole)
    
    registerAggregationTransitions(ticket, subSubMole)
    
    var size = 0
        
    val endTask = end.capsule.task.getOrElse(throw new InternalProcessingError("Capsule is empty"))
    
    for(value <- values) {
      size += 1
      subSubMole.incNbJobInProgress(1)

      val newTicket = moleExecution.nextTicket(ticket)
      val destContext = new Context

      val notFound = new ListBuffer[IData[_]]

      for (in <- endTask.inputs) {
        context.variable(in.prototype) match {
          case None => notFound += in
          case Some(v) => destContext += v      
        }
      }
      
      val valueMap = value.groupBy{_.prototype.name}
      
      for(data <- notFound) {
        val prototype = data.prototype
        valueMap.get(prototype.name) match {
          case Some(variable) =>
            if(variable.size > 1) Logger.getLogger(classOf[ExplorationTransition].getName).warning("Misformed sampling prototype " + prototype + " has been found " + variable.size + " times in a single row.") 
            val value = variable.head.value
            if(prototype.accepts(value))
              destContext += (prototype.asInstanceOf[IPrototype[Any]], value)
            else throw new UserBadDataError("Found value of type " + value.asInstanceOf[AnyRef].getClass + " incompatible with prototype " + prototype)
          case None =>
        }
      }
 
      submitNextJobsIfReady(ContextBuffer(destContext, toClone), newTicket, subSubMole)
    }

    subSubMole.decNbJobInProgress(size)
  }

  override protected def plugStart = start.addOutputTransition(this)
  
  private def registerAggregationTransitions(ticket: ITicket, subMoleExecution: ISubMoleExecution) = {
    val alreadySeen = new HashSet[IGenericCapsule]
    val toProcess = new ListBuffer[(IGenericCapsule,Int)]
    toProcess += ((end.capsule,0))

    while(!toProcess.isEmpty) {
      val cur = toProcess.remove(0)
      val capsule = cur._1
      val level = cur._2
      
      if(!alreadySeen(capsule)) {
        alreadySeen += capsule
      
        capsule.outputTransitions.foreach {
          _ match {
            case t: IExplorationTransition => toProcess += ((t.end.capsule, level + 1))
            case t: IAggregationTransition => 
              if(level > 0) toProcess += ((t.end.capsule, level - 1))
              else if(level == 0) {
                subMoleExecution.aggregationTransitionRegistry.register(t, ticket, new ContextBuffer)
                EventDispatcher.registerForObjectChangedSynchronous(subMoleExecution, Priority.LOW, new AggregationTransitionAdapter(t), ISubMoleExecution.Finished)
              }
            case t => toProcess += ((t.end.capsule, level))
          }
        }
      }
    }
  }
  
}
