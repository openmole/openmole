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

package org.openmole.core.implementation.transition

import org.openmole.misc.exception._
import org.openmole.core.implementation.tools._
import org.openmole.core.model.mole._
import org.openmole.core.model.data._
import org.openmole.core.model.job._
import org.openmole.core.model.task._
import org.openmole.core.model.transition._
import org.openmole.core.model.tools._
import org.openmole.core.model.transition.ICondition._
import org.openmole.misc.tools.obj.ClassUtils._
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer
import org.openmole.misc.tools.service.LockUtil._

class AggregationTransition(start: ICapsule, end: Slot, condition: ICondition = True, filter: Filter[String] = Filter.empty, trigger: ICondition = ICondition.False) extends Transition(start, end, condition, filter) with IAggregationTransition {

  override def _perform(context: Context, ticket: ITicket, subMole: ISubMoleExecution) = {
    val moleExecution = subMole.moleExecution
    val mole = moleExecution.mole
    val parentTicket = ticket.parent.getOrElse(throw new UserBadDataError("Aggregation transition should take place after an exploration."))

    if (!subMole.canceled && !hasBeenPerformed(subMole, parentTicket)) {
      subMole.aggregationTransitionRegistry.consult(this, parentTicket) match {
        case Some(results) ⇒
          results ++= context.values

          if (trigger != ICondition.False) {
            val toArrayManifests = Map.empty[String, Manifest[_]] ++ start.outputs(mole, moleExecution.sources, moleExecution.hooks).toList.map { d ⇒ d.prototype.name -> d.prototype.`type` }
            val context = ContextAggregator.aggregate(start.outputs(mole, moleExecution.sources, moleExecution.hooks), toArrayManifests, results.toIterable)
            if (trigger.evaluate(context)) {
              aggregate(subMole, ticket)
              if (allAggregationTransitionsPerformed(subMole, parentTicket)) subMole.cancel
            }
          }

        case None ⇒ throw new InternalProcessingError("No context registred for aggregation.")
      }
    }
  }

  override def aggregate(subMole: ISubMoleExecution, ticket: ITicket) = subMole.transitionLock {
    val parentTicket = ticket.parent.getOrElse(throw new UserBadDataError("Aggregation transition should take place after an exploration"))

    if (!subMole.canceled && !hasBeenPerformed(subMole, parentTicket)) {
      val result = subMole.aggregationTransitionRegistry.remove(this, parentTicket).getOrElse(throw new InternalProcessingError("No context registred for the aggregation transition"))
      val subMoleParent = subMole.parent.getOrElse(throw new InternalProcessingError("Submole execution has no parent"))
      subMoleParent.transitionLock { submitNextJobsIfReady(result, parentTicket, subMoleParent) }
    }
  }

  override def hasBeenPerformed(subMole: ISubMoleExecution, ticket: ITicket) = !subMole.aggregationTransitionRegistry.isRegistred(this, ticket)

  protected def allAggregationTransitionsPerformed(subMole: ISubMoleExecution, ticket: ITicket) = !oneAggregationTransitionNotPerformed(subMole, ticket)

  private def oneAggregationTransitionNotPerformed(subMole: ISubMoleExecution, ticket: ITicket): Boolean = {
    val mole = subMole.moleExecution.mole
    val alreadySeen = new HashSet[ICapsule]
    val toProcess = new ListBuffer[(ICapsule, Int)]
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
