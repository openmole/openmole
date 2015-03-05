/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.workflow.transition

import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.tools.service.{ Logger, LockUtil }
import org.openmole.core.workflow.validation.TypeUtil
import org.openmole.core.workflow.tools.ContextAggregator._
import org.openmole.core.workflow.tools._
import TypeUtil._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.transition._
import scala.collection.mutable.Buffer
import scala.collection.mutable.ListBuffer
import LockUtil._

object Transition extends Logger

import Transition.Log._

class Transition(
    val start: Capsule,
    val end: Slot,
    val condition: Condition = Condition.True,
    val filter: Filter[String] = Filter.empty) extends ITransition {

  private def nextTaskReady(ticket: Ticket, subMole: SubMoleExecution): Boolean = {
    val registry = subMole.transitionRegistry
    val mole = subMole.moleExecution.mole
    mole.inputTransitions(end).forall(registry.isRegistred(_, ticket))
  }

  protected def submitNextJobsIfReady(context: Iterable[Variable[_]], ticket: Ticket, subMole: SubMoleExecution) = {
    val moleExecution = subMole.moleExecution
    val registry = subMole.transitionRegistry
    val mole = subMole.moleExecution.mole

    registry.register(this, ticket, context)
    if (nextTaskReady(ticket, subMole)) {
      val dataChannelVariables = mole.inputDataChannels(end).toList.flatMap { _.consums(ticket, moleExecution) }

      def removeVariables(t: ITransition) = registry.remove(t, ticket).getOrElse(throw new InternalProcessingError("BUG context should be registred")).toIterable

      val transitionsVariables: Iterable[Variable[_]] =
        mole.inputTransitions(end).toList.flatMap {
          t ⇒ removeVariables(t)
        }

      val combinaison: Iterable[Variable[_]] = dataChannelVariables ++ transitionsVariables

      val newTicket =
        if (mole.slots(end.capsule).size <= 1) ticket
        else moleExecution.nextTicket(ticket.parent.getOrElse(throw new InternalProcessingError("BUG should never reach root ticket")))

      val toArrayManifests =
        Map.empty[String, Manifest[_]] ++
          computeManifests(mole, moleExecution.sources, moleExecution.hooks)(end).
          filter(_.toArray).map(ct ⇒ ct.name -> ct.manifest)

      val newContext = aggregate(end.capsule.inputs(mole, moleExecution.sources, moleExecution.hooks), toArrayManifests, combinaison)

      subMole.submit(end.capsule, newContext, newTicket)
    }
  }

  override def perform(context: Context, ticket: Ticket, subMole: SubMoleExecution) =
    try {
      if (isConditionTrue(context)) _perform(context, ticket, subMole)
    }
    catch {
      case e: Throwable ⇒
        logger.log(SEVERE, "Error in " + this, e)
        throw e
    }

  override def isConditionTrue(context: Context): Boolean = condition.evaluate(context)

  override def data(mole: Mole, sources: Sources, hooks: Hooks) =
    start.outputs(mole, sources, hooks).filterNot(d ⇒ filter(d.prototype.name))

  protected def _perform(context: Context, ticket: Ticket, subMole: SubMoleExecution) = submitNextJobsIfReady(ListBuffer() ++ filtered(context).values, ticket, subMole)
  protected def filtered(context: Context) = context.filterNot { case (n, _) ⇒ filter(n) }

  override def toString = this.getClass.getSimpleName + " from " + start + " to " + end

}
