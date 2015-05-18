/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.core.workflow.transition

import org.openmole.core.eventdispatcher._
import org.openmole.core.exception._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.task._
import org.openmole.tool.lock._

import scala.collection.mutable.{ HashSet, ListBuffer }
import scala.util.Random

class ExplorationTransition(start: Capsule, end: Slot, condition: Condition = Condition.True, filter: Filter[String] = Filter.empty) extends Transition(start, end, condition, filter) with IExplorationTransition {

  override def _perform(context: Context, ticket: Ticket, subMole: SubMoleExecution)(implicit rng: RandomProvider) = {
    val subSubMole = subMole.newChild

    registerAggregationTransitions(ticket, subSubMole)
    subSubMole.transitionLock { submitIn(filtered(context), ticket, subSubMole) }
  }

  def submitIn(context: Context, ticket: Ticket, subMole: SubMoleExecution)(implicit rng: RandomProvider) = {
    val moleExecution = subMole.moleExecution
    val mole = moleExecution.mole
    def explored = ExplorationTask.explored(start)
    val (factors, outputs) = start.outputs(mole, moleExecution.sources, moleExecution.hooks).partition(explored)

    val typedFactors = factors.map(_.asInstanceOf[Prototype[Array[Any]]])
    val values = typedFactors.toList.map(context(_).toIterable).transpose

    for (value ← values) {
      val newTicket = subMole.moleExecution.nextTicket(ticket)
      val variables = new ListBuffer[Variable[_]]

      for (in ← outputs)
        context.variable(in) match {
          case Some(v) ⇒ variables += v
          case None    ⇒
        }

      for ((p, v) ← typedFactors zip value) {
        val fp = p.fromArray
        if (fp.accepts(v)) variables += Variable(fp, v)
        else throw new UserBadDataError("Found value of type " + v.asInstanceOf[AnyRef].getClass + " incompatible with prototype " + fp)
      }
      submitNextJobsIfReady(ListBuffer() ++ variables, newTicket, subMole)
    }

  }

  private def registerAggregationTransitions(ticket: Ticket, subMoleExecution: SubMoleExecution) = {
    val alreadySeen = new HashSet[Capsule]
    val toProcess = new ListBuffer[(Capsule, Int)]
    toProcess += ((end.capsule, 0))

    while (!toProcess.isEmpty) {
      val cur = toProcess.remove(0)
      val capsule = cur._1
      val level = cur._2

      if (!alreadySeen(capsule)) {
        alreadySeen += capsule

        subMoleExecution.moleExecution.mole.outputTransitions(capsule).foreach {
          case t: IAggregationTransition ⇒
            if (level > 0) toProcess += t.end.capsule -> (level - 1)
            else if (level == 0) {
              subMoleExecution.aggregationTransitionRegistry.register(t, ticket, new ListBuffer)
              subMoleExecution listen {
                case ev: SubMoleExecution.Finished ⇒ t.aggregate(subMoleExecution, ev.ticket)
              }
            }
          case t: IExplorationTransition ⇒ toProcess += t.end.capsule -> (level + 1)
          case t                         ⇒ toProcess += t.end.capsule -> level
        }
      }
    }
  }

}
