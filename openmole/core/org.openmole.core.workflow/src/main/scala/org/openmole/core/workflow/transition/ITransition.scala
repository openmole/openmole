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

import org.openmole.core.context._
import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.mole.MoleExecution.SubMoleExecutionState
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.tools.ContextAggregator
import org.openmole.core.workflow.validation.TypeUtil._

object ITransition {

  def nextTaskReady(end: TransitionSlot)(ticket: Ticket, registry: MoleExecution.TransitionRegistry, mole: Mole): Boolean = mole.inputTransitions(end).forall(registry.isRegistred(_, ticket))

  def submitNextJobsIfReady(transition: ITransition)(context: Iterable[Variable[_]], ticket: Ticket, subMoleState: SubMoleExecutionState) = {
    val mole = subMoleState.moleExecution.mole
    subMoleState.transitionRegistry.register(transition, ticket, context)
    if (nextTaskReady(transition.end)(ticket, subMoleState.transitionRegistry, mole)) {
      val dataChannelVariables = mole.inputDataChannels(transition.end).toList.flatMap { d ⇒ DataChannel.consums(d, ticket, subMoleState.moleExecution) }

      def removeVariables(t: ITransition) = subMoleState.transitionRegistry.remove(t, ticket).getOrElse(throw new InternalProcessingError("BUG context should be registered")).toIterable

      val transitionsVariables: Iterable[Variable[_]] =
        mole.inputTransitions(transition.end).toList.flatMap {
          t ⇒ removeVariables(t)
        }

      val combinasion = (dataChannelVariables ++ transitionsVariables)

      val newTicket =
        if (mole.slots(transition.end.capsule).size <= 1) ticket
        else MoleExecution.nextTicket(subMoleState.moleExecution, ticket.parent.getOrElse(throw new InternalProcessingError("BUG should never reach root ticket")))

      val toArrayManifests =
        validTypes(mole, subMoleState.moleExecution.sources, subMoleState.moleExecution.hooks)(transition.end).filter(_.toArray).map(ct ⇒ ct.name → ct.`type`).toMap[String, ValType[_]]

      val newContext = ContextAggregator.aggregate(transition.end.capsule.inputs(mole, subMoleState.moleExecution.sources, subMoleState.moleExecution.hooks), toArrayManifests, combinasion.map(ticket.content → _))
      MoleExecution.submit(subMoleState, transition.end.capsule, newContext, newTicket)
    }
  }
}

/**
 * The trait representing a transition between a start point which is a [[org.openmole.core.workflow.mole.MoleCapsule]]
 * and an endpoint which is a [[org.openmole.core.workflow.transition.TransitionSlot]]
 */
trait ITransition {

  /**
   *
   * Get the starting capsule of this transition.
   *
   * @return the starting capsule of this transition
   */
  def start: MoleCapsule

  /**
   *
   * Get the ending capsule of this transition.
   *
   * @return the ending capsule of this transition
   */
  def end: TransitionSlot

  /**
   *
   * Get the condition under which this transition is performed.
   *
   * @return the condition under which this transition is performed
   */
  //def condition: Condition

  /**
   *
   * Get the filter of the variables which are filtered by this transition.
   *
   * @return filter on the names of the variables which are filtered by this transition
   */
  def filter: BlockList

  /**
   * Get the unfiltered user output data of the starting capsule going through
   * this transition
   *
   * @return the unfiltred output data of the staring capsule
   */
  def data(mole: Mole, sources: Sources, hooks: Hooks): PrototypeSet =
    start.outputs(mole, sources, hooks).filterNot(d ⇒ filter(d))

  /**
   *
   * Perform the transition and submit the jobs for the following capsules in the mole.
   *
   * @param ticket    ticket of the previous job
   * @param subMole   current submole
   */
  def perform(context: Context, ticket: Ticket, moleExecution: MoleExecution, subMole: SubMoleExecution, moleExecutionContext: MoleExecutionContext): Unit

  /**
   * Filter a given context
   * @param context
   * @return
   */
  protected def filtered(context: Context): Context = context.filterNot { case (_, v) ⇒ filter(v.prototype) }

  override def toString = this.getClass.getSimpleName + " from " + start + " to " + end

}
