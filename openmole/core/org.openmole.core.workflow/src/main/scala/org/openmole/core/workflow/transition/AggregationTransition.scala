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

import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.tools.service.LockUtil
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.transition.Condition._
import LockUtil._

import scala.collection.mutable.{ HashSet, ListBuffer }

object AggregationTransition {
  def aggregateOutputs(moleExecution: MoleExecution, transition: IAggregationTransition, results: Iterable[Variable[_]]) = {
    val toArrayManifests = Map.empty[String, Manifest[_]] ++ transition.start.outputs(moleExecution.mole, moleExecution.sources, moleExecution.hooks).toList.map { d ⇒ d.prototype.name -> d.prototype.`type` }
    ContextAggregator.aggregate(transition.start.outputs(moleExecution.mole, moleExecution.sources, moleExecution.hooks), toArrayManifests, results)
  }
}

class AggregationTransition(start: Capsule, end: Slot, condition: Condition = True, filter: Filter[String] = Filter.empty, trigger: Condition = Condition.False) extends Transition(start, end, condition, filter) with IAggregationTransition {

  override def _perform(context: Context, ticket: Ticket, subMole: SubMoleExecution) = {
    val moleExecution = subMole.moleExecution
    val mole = moleExecution.mole
    val parentTicket = ticket.parent.getOrElse(throw new UserBadDataError("Aggregation transition should take place after an exploration."))

    if (!subMole.canceled && !hasBeenPerformed(subMole, parentTicket)) {
      subMole.aggregationTransitionRegistry.consult(this, parentTicket) match {
        case Some(results) ⇒
          results ++= filtered(context).values

          if (trigger != Condition.False) {
            val context = AggregationTransition.aggregateOutputs(moleExecution, this, results)
            if (trigger.evaluate(context)) {
              aggregate(subMole, ticket)
              if (allAggregationTransitionsPerformed(subMole, parentTicket)) subMole.cancel
            }
          }

        case None ⇒ throw new InternalProcessingError("No context registered for aggregation.")
      }
    }
  }

  override def aggregate(subMole: SubMoleExecution, ticket: Ticket) = subMole.transitionLock {
    val parentTicket = ticket.parent.getOrElse(throw new UserBadDataError("Aggregation transition should take place after an exploration"))

    if (!subMole.canceled && !hasBeenPerformed(subMole, parentTicket)) {
      val results = subMole.aggregationTransitionRegistry.remove(this, parentTicket).getOrElse(throw new InternalProcessingError("No context registred for the aggregation transition"))
      val subMoleParent = subMole.parent.getOrElse(throw new InternalProcessingError("Submole execution has no parent"))
      val aggregated = AggregationTransition.aggregateOutputs(subMole.moleExecution, this, results)
      subMoleParent.transitionLock { submitNextJobsIfReady(aggregated.values, parentTicket, subMoleParent) }
    }
  }

  override def hasBeenPerformed(subMole: SubMoleExecution, ticket: Ticket) = !subMole.aggregationTransitionRegistry.isRegistred(this, ticket)

  protected def allAggregationTransitionsPerformed(subMole: SubMoleExecution, ticket: Ticket) = !oneAggregationTransitionNotPerformed(subMole, ticket)

  private def oneAggregationTransitionNotPerformed(subMole: SubMoleExecution, ticket: Ticket): Boolean = {
    val mole = subMole.moleExecution.mole
    val alreadySeen = new HashSet[Capsule]
    val toProcess = new ListBuffer[(Capsule, Int)]
    toProcess += ((this.start, 0))

    while (!toProcess.isEmpty) {
      val (capsule, level) = toProcess.remove(0)

      if (!alreadySeen(capsule)) {
        alreadySeen += capsule
        mole.slots(capsule).toList.flatMap { mole.inputTransitions }.foreach {
          case t: IExplorationTransition ⇒ if (level > 0) toProcess += ((t.start, level - 1))
          case t: IAggregationTransition ⇒
            if (level == 0 && t != this && !t.hasBeenPerformed(subMole, ticket)) return true
            toProcess += ((t.start, level + 1))
          case t ⇒ toProcess += ((t.start, level))
        }
        mole.outputTransitions(capsule).foreach {
          case t: IExplorationTransition ⇒ toProcess += ((t.end.capsule, level + 1))
          case t: IAggregationTransition ⇒
            if (level == 0 && t != this && !t.hasBeenPerformed(subMole, ticket)) return true
            if (level > 0) toProcess += ((t.end.capsule, level - 1))
          case t ⇒ toProcess += ((t.end.capsule, level))
        }
      }
    }
    false
  }

}
