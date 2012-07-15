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
import org.openmole.core.implementation.tools.ContextAggregator._
import org.openmole.core.implementation.validation.TypeUtil._
import org.openmole.core.implementation.data.Context._
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.mole.{ ICapsule, ITicket, ISubMoleExecution }
import org.openmole.core.model.data.IContext
import org.openmole.core.model.transition.{ ICondition, ITransition, ISlot }
import org.openmole.core.implementation.mole.Capsule._
import org.openmole.misc.tools.service.LockRepository
import org.openmole.misc.tools.service.Logger
import scala.collection.mutable.Buffer
import scala.collection.mutable.ListBuffer

object Transition extends Logger

import Transition._

class Transition(
    val start: ICapsule,
    val end: ISlot,
    val condition: ICondition = ICondition.True,
    val filtered: Iterable[String] = Iterable.empty[String]) extends ITransition {

  start.addOutputTransition(this)
  end += this

  @transient val filteredSet = filtered.toSet

  private def nextTaskReady(ticket: ITicket, subMole: ISubMoleExecution): Boolean = {
    val registry = subMole.transitionRegistry
    !end.transitions.exists(!registry.isRegistred(_, ticket))
  }

  protected def submitNextJobsIfReady(context: Buffer[IVariable[_]], ticket: ITicket, subMole: ISubMoleExecution) = subMole.transitionRegistry.synchronized {
    import subMole.moleExecution
    val registry = subMole.transitionRegistry
    registry.register(this, ticket, context)
    if (nextTaskReady(ticket, subMole)) {
      val combinaison =
        end.inputDataChannels.toList.flatMap { _.consums(ticket, moleExecution) } ++
          end.transitions.toList.flatMap(registry.remove(_, ticket).getOrElse(throw new InternalProcessingError("BUG context should be registred")).toIterable)

      val newTicket =
        if (end.capsule.intputSlots.size <= 1) ticket
        else moleExecution.nextTicket(ticket.parent.getOrElse(throw new InternalProcessingError("BUG should never reach root ticket")))

      val toAggregate = combinaison.groupBy(_.prototype.name)

      val toArrayManifests =
        Map.empty[String, Manifest[_]] ++ computeManifests(end).filter(_.toArray).map(ct ⇒ ct.name -> ct.manifest)

      val newContext = aggregate(end.capsule.inputs, toArrayManifests, combinaison)
      subMole.submit(end.capsule, newContext, newTicket)
    }
  }

  override def perform(context: IContext, ticket: ITicket, subMole: ISubMoleExecution) =
    if (isConditionTrue(context)) _perform(context -- filtered, ticket, subMole)

  override def isConditionTrue(context: IContext): Boolean = condition.evaluate(context)

  override def data = start.outputs.filterNot(d ⇒ filteredSet.contains(d.prototype.name))

  protected def _perform(context: IContext, ticket: ITicket, subMole: ISubMoleExecution) = submitNextJobsIfReady(ListBuffer() ++ context.values, ticket, subMole)

  override def toString = "Transition from " + start + " to " + end

}
