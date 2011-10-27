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

import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.tools.ContextAggregator
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.mole.ISubMoleExecution
import org.openmole.core.model.mole.ITicket
import org.openmole.core.model.task.ITask
import org.openmole.core.model.transition.IExplorationTransition
import org.openmole.core.model.transition.IMasterTransition
import org.openmole.core.model.transition.IAggregationTransition
import org.openmole.core.model.transition.ICondition
import org.openmole.core.model.transition.ICondition._
import org.openmole.core.model.transition.ISlot
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.exception.UserBadDataError
import org.openmole.core.implementation.data.DataMode
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.implementation.data.Prototype._
import org.openmole.core.implementation.data.Context
import org.openmole.core.implementation.data.Context._
import org.openmole.misc.tools.obj.ClassUtils._
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer

class MasterTransition(val selection: ITask, start: ICapsule, val master: ISlot, end: ISlot, condition: ICondition = True, filtered: Set[String] = Set.empty[String], trigger: Option[ICondition] = None) extends AggregationTransition(start, end, condition, filtered, trigger) with IMasterTransition {
  
  def this(selection: ITask, start: ICapsule, master: ISlot, end: ICapsule) = this(selection, start, master, end.defaultInputSlot, True, Set.empty[String], None)
    
  def this(selection: ITask, start: ICapsule, master: ISlot, end: ICapsule, condition: ICondition) = this(selection, start, master, end.defaultInputSlot, condition, Set.empty[String], None)

  def this(selection: ITask, start: ICapsule, master: ISlot, end: ICapsule, condition: String) = this(selection, start, master, end.defaultInputSlot, new Condition(condition), Set.empty[String], None)
    
  def this(selection: ITask, start: ICapsule, master: ISlot, slot: ISlot, condition: String) = this(selection,start,  master, slot, new Condition(condition), Set.empty[String], None)
    
  def this(selection: ITask, start: ICapsule, master: ISlot, slot: ISlot, condition: ICondition) = this(selection, start, master, slot, condition, Set.empty[String], None)
   
  def this(selection: ITask, start: ICapsule, master: ISlot, end: ICapsule, filtred: Array[String]) = this(selection, start, master, end.defaultInputSlot, ICondition.True, filtred.toSet, None)
    
  def this(selection: ITask, start: ICapsule, master: ISlot, end: ICapsule, condition: ICondition, filtred: Array[String]) = this(selection, start, master, end.defaultInputSlot, condition, filtred.toSet, None)

  def this(selection: ITask, start: ICapsule, master: ISlot, end: ICapsule, condition: String, filtred: Array[String]) = this(selection, start, master, end.defaultInputSlot, new Condition(condition), filtred.toSet, None)
    
  def this(selection: ITask, start: ICapsule, master: ISlot, slot: ISlot, condition: String, filtred: Array[String]) = this(selection, start, master, slot, new Condition(condition), filtred.toSet, None)

  
  def this(selection: ITask, trigger: ICondition, start: ICapsule, master: ISlot, end: ICapsule) = this(selection, start, master, end.defaultInputSlot, ICondition.True, Set.empty[String], Some(trigger))
    
  def this(selection: ITask, trigger: ICondition, start: ICapsule, master: ISlot, end: ICapsule, condition: ICondition) = this(selection, start, master, end.defaultInputSlot, condition, Set.empty[String], Some(trigger))

  def this(selection: ITask, trigger: ICondition, start: ICapsule, master: ISlot, end: ICapsule, condition: String) = this(selection, start, master, end.defaultInputSlot, new Condition(condition), Set.empty[String], Some(trigger))
    
  def this(selection: ITask, trigger: ICondition, start: ICapsule, master: ISlot, slot: ISlot, condition: String) = this(selection, start, master, slot, new Condition(condition), Set.empty[String], Some(trigger))
    
  def this(selection: ITask, trigger: ICondition, start: ICapsule, master: ISlot, slot: ISlot, condition: ICondition) = this(selection, start, master, slot, condition, Set.empty[String], Some(trigger))
   
  def this(selection: ITask, trigger: ICondition, start: ICapsule, master: ISlot, end: ICapsule, filtred: Array[String]) = this(selection, start, master, end.defaultInputSlot, ICondition.True, filtred.toSet, Some(trigger))
    
  def this(selection: ITask, trigger: ICondition, start: ICapsule, master: ISlot, end: ICapsule, condition: ICondition, filtred: Array[String]) = this(selection, start, master, end.defaultInputSlot, condition, filtred.toSet, Some(trigger))

  def this(selection: ITask, trigger: ICondition, start: ICapsule, master: ISlot, end: ICapsule, condition: String, filtred: Array[String]) = this(selection, start, master, end.defaultInputSlot, new Condition(condition), filtred.toSet, Some(trigger))
    
  def this(selection: ITask, trigger: ICondition, start: ICapsule, master: ISlot, slot: ISlot, condition: String, filtred: Array[String]) = this(selection, start, master, slot, new Condition(condition), filtred.toSet, Some(trigger))
  

  def this(selection: ITask, trigger: String, start: ICapsule, master: ISlot, end: ICapsule) = this(selection, new Condition(trigger), start, master, end)  
  
  def this(selection: ITask, trigger: String, start: ICapsule, master: ISlot, end: ICapsule, condition: ICondition) = this(selection,new Condition(trigger), start, master, end, condition)

  def this(selection: ITask, trigger: String, start: ICapsule, master: ISlot, end: ICapsule, condition: String) = this(selection, new Condition(trigger), start, master, end, condition)
  
  def this(selection: ITask, trigger: String, start: ICapsule, master: ISlot, slot: ISlot, condition: String) = this(selection, new Condition(trigger), start, master, slot, condition)
    
  def this(selection: ITask, trigger: String, start: ICapsule, master: ISlot, slot: ISlot, condition: ICondition) = this(selection, new Condition(trigger), start, master, slot, condition)
   
  def this(selection: ITask, trigger: String, start: ICapsule, master: ISlot, end: ICapsule, filtred: Array[String]) = this(selection, new Condition(trigger), start, master, end, filtred)
    
  def this(selection: ITask, trigger: String, start: ICapsule, master: ISlot, end: ICapsule, condition: ICondition, filtred: Array[String]) = this(selection, new Condition(trigger), start, master, end, condition, filtred)

  def this(selection: ITask, trigger: String, start: ICapsule, master: ISlot, end: ICapsule, condition: String, filtred: Array[String]) = this(selection, new Condition(trigger), start, master, end, condition, filtred)
    
  def this(selection: ITask, trigger: String, start: ICapsule, master: ISlot, slot: ISlot, condition: String, filtred: Array[String]) = this(selection, new Condition(trigger), start, master, slot, condition, filtred)

  
  
  def this(selection: ITask, start: ICapsule, master: ICapsule, end: ICapsule) = this(selection, start, master.defaultInputSlot, end.defaultInputSlot, True, Set.empty[String], None)
    
  def this(selection: ITask, start: ICapsule, master: ICapsule, end: ICapsule, condition: ICondition) = this(selection, start, master.defaultInputSlot, end.defaultInputSlot, condition, Set.empty[String], None)

  def this(selection: ITask, start: ICapsule, master: ICapsule, end: ICapsule, condition: String) = this(selection, start, master.defaultInputSlot, end.defaultInputSlot, new Condition(condition), Set.empty[String], None)
    
  def this(selection: ITask, start: ICapsule, master: ICapsule, slot: ISlot, condition: String) = this(selection,start,  master.defaultInputSlot, slot, new Condition(condition), Set.empty[String], None)
    
  def this(selection: ITask, start: ICapsule, master: ICapsule, slot: ISlot, condition: ICondition) = this(selection, start, master.defaultInputSlot, slot, condition, Set.empty[String], None)
   
  def this(selection: ITask, start: ICapsule, master: ICapsule, end: ICapsule, filtred: Array[String]) = this(selection, start, master.defaultInputSlot, end.defaultInputSlot, ICondition.True, filtred.toSet, None)
    
  def this(selection: ITask, start: ICapsule, master: ICapsule, end: ICapsule, condition: ICondition, filtred: Array[String]) = this(selection, start, master.defaultInputSlot, end.defaultInputSlot, condition, filtred.toSet, None)

  def this(selection: ITask, start: ICapsule, master: ICapsule, end: ICapsule, condition: String, filtred: Array[String]) = this(selection, start, master.defaultInputSlot, end.defaultInputSlot, new Condition(condition), filtred.toSet, None)
    
  def this(selection: ITask, start: ICapsule, master: ICapsule, slot: ISlot, condition: String, filtred: Array[String]) = this(selection, start, master.defaultInputSlot, slot, new Condition(condition), filtred.toSet, None)

  
  def this(selection: ITask, trigger: ICondition, start: ICapsule, master: ICapsule, end: ICapsule) = this(selection, start, master.defaultInputSlot, end.defaultInputSlot, ICondition.True, Set.empty[String], Some(trigger))
    
  def this(selection: ITask, trigger: ICondition, start: ICapsule, master: ICapsule, end: ICapsule, condition: ICondition) = this(selection, start, master.defaultInputSlot, end.defaultInputSlot, condition, Set.empty[String], Some(trigger))

  def this(selection: ITask, trigger: ICondition, start: ICapsule, master: ICapsule, end: ICapsule, condition: String) = this(selection, start, master.defaultInputSlot, end.defaultInputSlot, new Condition(condition), Set.empty[String], Some(trigger))
    
  def this(selection: ITask, trigger: ICondition, start: ICapsule, master: ICapsule, slot: ISlot, condition: String) = this(selection, start, master.defaultInputSlot, slot, new Condition(condition), Set.empty[String], Some(trigger))
    
  def this(selection: ITask, trigger: ICondition, start: ICapsule, master: ICapsule, slot: ISlot, condition: ICondition) = this(selection, start, master.defaultInputSlot, slot, condition, Set.empty[String], Some(trigger))
   
  def this(selection: ITask, trigger: ICondition, start: ICapsule, master: ICapsule, end: ICapsule, filtred: Array[String]) = this(selection, start, master.defaultInputSlot, end.defaultInputSlot, ICondition.True, filtred.toSet, Some(trigger))
    
  def this(selection: ITask, trigger: ICondition, start: ICapsule, master: ICapsule, end: ICapsule, condition: ICondition, filtred: Array[String]) = this(selection, start, master.defaultInputSlot, end.defaultInputSlot, condition, filtred.toSet, Some(trigger))

  def this(selection: ITask, trigger: ICondition, start: ICapsule, master: ICapsule, end: ICapsule, condition: String, filtred: Array[String]) = this(selection, start, master.defaultInputSlot, end.defaultInputSlot, new Condition(condition), filtred.toSet, Some(trigger))
    
  def this(selection: ITask, trigger: ICondition, start: ICapsule, master: ICapsule, slot: ISlot, condition: String, filtred: Array[String]) = this(selection, start, master.defaultInputSlot, slot, new Condition(condition), filtred.toSet, Some(trigger))
  

  def this(selection: ITask, trigger: String, start: ICapsule, master: ICapsule, end: ICapsule) = this(selection, new Condition(trigger), start, master.defaultInputSlot, end)  
  
  def this(selection: ITask, trigger: String, start: ICapsule, master: ICapsule, end: ICapsule, condition: ICondition) = this(selection,new Condition(trigger), start, master.defaultInputSlot, end, condition)

  def this(selection: ITask, trigger: String, start: ICapsule, master: ICapsule, end: ICapsule, condition: String) = this(selection, new Condition(trigger), start, master.defaultInputSlot, end, condition)
  
  def this(selection: ITask, trigger: String, start: ICapsule, master: ICapsule, slot: ISlot, condition: String) = this(selection, new Condition(trigger), start, master.defaultInputSlot, slot, condition)
    
  def this(selection: ITask, trigger: String, start: ICapsule, master: ICapsule, slot: ISlot, condition: ICondition) = this(selection, new Condition(trigger), start, master.defaultInputSlot, slot, condition)
   
  def this(selection: ITask, trigger: String, start: ICapsule, master: ICapsule, end: ICapsule, filtred: Array[String]) = this(selection, new Condition(trigger), start, master.defaultInputSlot, end, filtred)
    
  def this(selection: ITask, trigger: String, start: ICapsule, master: ICapsule, end: ICapsule, condition: ICondition, filtred: Array[String]) = this(selection, new Condition(trigger), start, master.defaultInputSlot, end, condition, filtred)

  def this(selection: ITask, trigger: String, start: ICapsule, master: ICapsule, end: ICapsule, condition: String, filtred: Array[String]) = this(selection, new Condition(trigger), start, master.defaultInputSlot, end, condition, filtred)
    
  def this(selection: ITask, trigger: String, start: ICapsule, master: ICapsule, slot: ISlot, condition: String, filtred: Array[String]) = this(selection, new Condition(trigger), start, master.defaultInputSlot, slot, condition, filtred)

  override def _perform(context: IContext, ticket: ITicket, subMole: ISubMoleExecution) = subMole.synchronized {

    val parentTicket = ticket.parent.getOrElse(throw new UserBadDataError("Aggregation transition should take place after an exploration."))
    if(!hasBeenPerformed(subMole, parentTicket)) {
      subMole.aggregationTransitionRegistry.remove(this, parentTicket) match {
        case Some(resultVariables) =>
          val selectedContext = subMole.masterTransitionRegistry.remove(this, parentTicket).getOrElse(new Context)
          
          //Manualy aggregate to ensure the last result is at the head
          val grouped = resultVariables.groupBy(_.prototype.name)
          val aggregated = start.outputs.toList.map { 
            out =>
            val group = grouped.getOrElse(out.prototype.name, List())
            val array = out.prototype.`type`.newArray(group.size + 1)
            group.zipWithIndex.foreach{e => java.lang.reflect.Array.set(array, e._2 + 1, e._1.value)}
            java.lang.reflect.Array.set(array, 0, context.value(out.prototype.name).get)
            new Variable(out.prototype.name, array)
          }.toContext
          
          val selectedNewContext = selection.perform(selectedContext ++ aggregated)
          
          val variables = start.outputs.toList.flatMap {
            out =>
              selectedNewContext.value(toArray(out.prototype)) match {
                case Some(v) => v.map{x => new Variable(out.prototype.asInstanceOf[IPrototype[Any]], x)}
                case None => throw new UserBadDataError("In master transition " + this + " selection task should have an array of variable " + out.prototype + " in output.")
              } 
          }
          
          subMole.aggregationTransitionRegistry.register(this, parentTicket, ListBuffer[IVariable[_]]() ++ variables)
          //println("job in prog " + subMole.nbJobInProgess)
          trigger match {
            case Some(trigger) => 
              if(trigger.evaluate(selectedNewContext)) {
                aggregate(subMole, ticket)
                if(allAggregationTransitionsPerformed(subMole, parentTicket)) subMole.cancel
              }
            case None =>
          }
          
          subMole.submit(master.capsule, selectedNewContext, subMole.moleExecution.nextTicket(parentTicket))
          if(!hasBeenPerformed(subMole, parentTicket)) subMole.masterTransitionRegistry.register(this, parentTicket, selectedNewContext)
        case None => throw new InternalProcessingError("No context registred for aggregation.")
      }
    }
  }

  /*def matchingExploration: IExplorationTransition = {
    val alreadySeen = new HashSet[ICapsule]
    val toProcess = new ListBuffer[(ICapsule,Int)]
    
    toProcess += this.start -> 0
    
    while(!toProcess.isEmpty) {
      val (capsule, level) = toProcess.remove(0)
      if(!alreadySeen(capsule)) {
        for(s <- capsule.intputSlots; t <- s.transitions)
          t match {
            case t: IExplorationTransition => if(level <= 0) return t else toProcess += t.start -> (level - 1)
            case t: IAggregationTransition => toProcess += t.start -> (level + 1)
            case _ => toProcess += t.start -> level
          }
        alreadySeen += capsule
      }
      
    }
    throw new InternalProcessingError("No matching exploration transition has been found for master transition " + this)
  }*/

}
