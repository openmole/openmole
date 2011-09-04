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

package org.openmole.core.implementation.transition

import org.openmole.misc.exception.InternalProcessingError
import org.openmole.core.implementation.tools.VariablesBuffer
import org.openmole.core.implementation.tools.ContextAggregator._
import org.openmole.core.implementation.tools.TypeUtil._
import org.openmole.core.implementation.data.Context._
import org.openmole.core.model.mole.{ICapsule, ITicket, ISubMoleExecution}
import org.openmole.core.model.data.IContext
import org.openmole.core.model.task.{ITask, IExplorationTask}
import org.openmole.core.model.tools.IVariablesBuffer
import org.openmole.core.model.transition.{ICondition, ITransition, ISlot}
import org.openmole.core.implementation.mole.Capsule._
import org.openmole.misc.tools.service.LockRepository

object Transition {
  val lockRepository = new LockRepository[(ISlot, ISubMoleExecution, ITicket)]
}

class Transition(val start: ICapsule, val end: ISlot, val condition: ICondition, val filtered: Set[String]) extends ITransition {

  def this(start: ICapsule, end: ICapsule) = this(start, end.defaultInputSlot, ICondition.True, Set.empty[String])
    
  def this(start: ICapsule, end: ICapsule, condition: ICondition) = this(start, end.defaultInputSlot, condition, Set.empty[String])

  def this(start: ICapsule, end: ICapsule, condition: String) = this(start, end.defaultInputSlot, new Condition(condition), Set.empty[String])

  def this(start: ICapsule, slot: ISlot, condition: String) = this(start, slot, new Condition(condition), Set.empty[String])

  def this(start: ICapsule, slot: ISlot, condition: ICondition) = this(start, slot, condition, Set.empty[String])

  def this(start: ICapsule, end: ICapsule, filtred: Array[String]) = this(start, end.defaultInputSlot, ICondition.True, filtred.toSet)

  def this(start: ICapsule, end: ICapsule, condition: ICondition, filtred: Array[String]) = this(start, end.defaultInputSlot, condition, filtred.toSet)

  def this(start: ICapsule, end: ICapsule, condition: String, filtred: Array[String]) = this(start, end.defaultInputSlot, new Condition(condition), filtred.toSet)

  def this(start: ICapsule, slot: ISlot, condition: String, filtred: Array[String]) = this(start, slot, new Condition(condition), filtred.toSet)

  import Transition._
  
  start.addOutputTransition(this)
  end += this

  private def nextTaskReady(ticket: ITicket, subMole: ISubMoleExecution): Boolean = {
    val registry = subMole.transitionRegistry
    !end.transitions.exists(!registry.isRegistred(_, ticket))
  }
  
  protected def submitNextJobsIfReady(context: IVariablesBuffer, ticket: ITicket, subMole: ISubMoleExecution) = {
    val lockKey = (end, subMole, ticket)
    lockRepository.lock(lockKey)
    try {
      import subMole.moleExecution
      val registry = subMole.transitionRegistry
      registry.register(this, ticket, context)

      if (nextTaskReady(ticket, subMole)) {
        val combinaison = end.capsule.inputDataChannels.toList.flatMap{_.consums(ticket, moleExecution)} ++ 
                          end.transitions.toList.flatMap(registry.remove(_, ticket).getOrElse(throw new InternalProcessingError("BUG context should be registred")).toIterable)
                        
        val newTicket = 
          if (end.capsule.intputSlots.size <= 1) ticket 
          else moleExecution.nextTicket(ticket.parent.getOrElse(throw new InternalProcessingError("BUG should never reach root ticket")))

        val toAggregate = combinaison.groupBy(_.prototype.name)
      
        val toArrayManifests = Map.empty[String, Manifest[_]] ++ computeManifests(end).filter(_.toArray).map(ct => ct.name -> ct.manifest)
        val newContext = aggregate(end.capsule.inputs, toArrayManifests, combinaison)
        
        subMole.submit(end.capsule, newContext, newTicket)
      }
    } finally lockRepository.unlock(lockKey)
  }

  override def perform(context: IContext, ticket: ITicket, subMole: ISubMoleExecution) = {
    if (isConditionTrue(context)) {
      /*-- Remove filtred --*/
      _perform(context -- filtered, ticket, subMole)
    }
  }

  override def isConditionTrue(context: IContext): Boolean = condition.evaluate(context)

  override def unFiltred = start.outputs.filterNot(d => filtered.contains(d.prototype.name))
  
  protected def _perform(context: IContext, ticket: ITicket, subMole: ISubMoleExecution) = submitNextJobsIfReady(VariablesBuffer(context), ticket, subMole)

}
