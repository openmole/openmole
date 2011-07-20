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

import org.openmole.core.implementation.task.ExplorationTask._
import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.data.Context._
import org.openmole.core.implementation.mole.SubMoleExecution
import org.openmole.core.model.transition.{IExplorationTransition, ISlot, ICondition, IAggregationTransition}
import org.openmole.core.implementation.tools.ContextBuffer
import org.openmole.core.model.data.{IPrototype, IContext, IVariable, IData}
import org.openmole.core.model.mole.{ICapsule, ITicket, ISubMoleExecution}
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.tools.service.Priority
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer

object ExplorationTransition extends Logger

class ExplorationTransition(start: ICapsule, end: ISlot, condition: ICondition, filtered: Set[String], sampling: IPrototype[SampledValues] = Sample.prototype) extends Transition(start, end, condition, filtered) with IExplorationTransition {
  import ExplorationTransition._
  
  def this(start: ICapsule, end: ICapsule) = this(start, end.defaultInputSlot, ICondition.True, Set.empty[String])
    
  def this(start: ICapsule, end: ICapsule, condition: ICondition) = this(start, end.defaultInputSlot, condition, Set.empty[String])

  def this(start: ICapsule, end: ICapsule, condition: String) = this(start, end.defaultInputSlot, new Condition(condition), Set.empty[String])

  def this(start: ICapsule , slot: ISlot, condition: String) = this(start, slot, new Condition(condition), Set.empty[String])

  def this(start: ICapsule , slot: ISlot, condition: ICondition) = this(start, slot, condition, Set.empty[String])

  def this(start: ICapsule, end: ICapsule, filtred: Array[String]) = this(start, end.defaultInputSlot, ICondition.True, filtred.toSet)

  def this(start: ICapsule, end: ICapsule, condition: ICondition, filtred: Array[String]) = this(start, end.defaultInputSlot, condition, filtred.toSet)

  def this(start: ICapsule, end: ICapsule, condition: String, filtred: Array[String]) = this(start, end.defaultInputSlot, new Condition(condition), filtred.toSet)

  def this(start: ICapsule , slot: ISlot, condition: String, filtred: Array[String]) = this(start, slot, new Condition(condition), filtred.toSet)

  
  def this(start: ICapsule, end: ICapsule, sampling: IPrototype[SampledValues]) = this(start, end.defaultInputSlot, ICondition.True, Set.empty[String], sampling)
    
  def this(start: ICapsule, end: ICapsule, condition: ICondition, sampling: IPrototype[SampledValues]) = this(start, end.defaultInputSlot, condition, Set.empty[String], sampling)

  def this(start: ICapsule, end: ICapsule, condition: String, sampling: IPrototype[SampledValues]) = this(start, end.defaultInputSlot, new Condition(condition), Set.empty[String], sampling)

  def this(start: ICapsule , slot: ISlot, condition: String, sampling: IPrototype[SampledValues]) = this(start, slot, new Condition(condition), Set.empty[String], sampling)

  def this(start: ICapsule , slot: ISlot, condition: ICondition, sampling: IPrototype[SampledValues]) = this(start, slot, condition, Set.empty[String], sampling)

  def this(start: ICapsule, end: ICapsule, filtred: Array[String], sampling: IPrototype[SampledValues]) = this(start, end.defaultInputSlot, ICondition.True, filtred.toSet, sampling)

  def this(start: ICapsule, end: ICapsule, condition: ICondition, filtred: Array[String], sampling: IPrototype[SampledValues]) = this(start, end.defaultInputSlot, condition, filtred.toSet, sampling)

  def this(start: ICapsule, end: ICapsule, condition: String, filtred: Array[String], sampling: IPrototype[SampledValues]) = this(start, end.defaultInputSlot, new Condition(condition), filtred.toSet, sampling)

  def this(start: ICapsule , slot: ISlot, condition: String, filtred: Array[String], sampling: IPrototype[SampledValues]) = this(start, slot, new Condition(condition), filtred.toSet, sampling)


  override def _perform(context: IContext, ticket: ITicket, toClone: Set[String], subMole: ISubMoleExecution) = {
    import subMole.moleExecution
    val values = context.value(sampling).getOrElse(throw new UserBadDataError("Sample not found in the prototype " + sampling +" for the exploration transition."))
    val subSubMole = SubMoleExecution(moleExecution, subMole)
    
    registerAggregationTransitions(ticket, subSubMole)
    var size = 0
        
    val endTask = end.capsule.task.getOrElse(throw new InternalProcessingError("Capsule is empty"))
 
    for(value <- values) {
      size += 1
      subSubMole.incNbJobInProgress(1)

      val newTicket = moleExecution.nextTicket(ticket)
      
      val variables = new ListBuffer[IVariable[_]]
      val notFound = new ListBuffer[IData[_]]
      
      for (in <- endTask.inputs) {
        context.variable(in.prototype) match {
          case None => notFound += in
          case Some(v) => variables += v      
        }
      }
      
      val valueMap = value.groupBy{_.prototype.name}

      for(data <- notFound) {
        val prototype = data.prototype
        valueMap.get(prototype.name) match {
          case Some(variable) =>
            if(variable.size > 1) logger.warning("Misformed sampling prototype " + prototype + " has been found " + variable.size + " times in a single row.") 
            val value = variable.head.value
 
            if(prototype.accepts(value)) variables += new Variable(prototype.asInstanceOf[IPrototype[Any]], value)
            else throw new UserBadDataError("Found value of type " + value.asInstanceOf[AnyRef].getClass + " incompatible with prototype " + prototype) 
          case None =>
        }
      }
      submitNextJobsIfReady(ContextBuffer(variables.toContext, toClone), newTicket, subSubMole)
    }

    subSubMole.decNbJobInProgress(size)
  }

  
  private def registerAggregationTransitions(ticket: ITicket, subMoleExecution: ISubMoleExecution) = {
    val alreadySeen = new HashSet[ICapsule]
    val toProcess = new ListBuffer[(ICapsule,Int)]
    toProcess += ((end.capsule,0))

    while(!toProcess.isEmpty) {
      val cur = toProcess.remove(0)
      val capsule = cur._1
      val level = cur._2
      
      if(!alreadySeen(capsule)) {
        alreadySeen += capsule
      
        capsule.outputTransitions.foreach {
          case t: IAggregationTransition => 
            if(level > 0) toProcess += t.end.capsule -> (level - 1)
            else if(level == 0) {
              subMoleExecution.aggregationTransitionRegistry.register(t, ticket, new ContextBuffer)
              EventDispatcher.registerForObjectChangedSynchronous(subMoleExecution, Priority.LOW, new AggregationTransitionAdapter(t), ISubMoleExecution.Finished)
            }
          case t: IExplorationTransition => toProcess += t.end.capsule -> (level + 1)
          case t => toProcess += t.end.capsule -> level
        }
      }
    }
  }
  
}
