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

import org.openmole.core.context.{ Context, Val, Variable }
import org.openmole.core.event._
import org.openmole.core.exception._
import org.openmole.core.expansion.{ Condition, FromContext }
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.validation.ValidateTransition
import org.openmole.core.workspace.NewFile
import org.openmole.tool.lock._

import scala.collection.mutable.{ HashSet, ListBuffer }

class ExplorationTransition(val start: Capsule, val end: Slot, val condition: Condition = Condition.True, val filter: BlockList = BlockList.empty) extends IExplorationTransition with ValidateTransition {

  override def validate(inputs: Seq[Val[_]]) = condition.validate(inputs)

  override def perform(context: Context, ticket: Ticket, subMole: SubMoleExecution, executionContext: MoleExecutionContext) = {
    val subSubMole = subMole.newChild
    registerAggregationTransitions(ticket, subSubMole, executionContext)
    subSubMole.transitionLock { submitIn(filtered(context), ticket, subSubMole, executionContext) }
  }

  def submitIn(context: Context, ticket: Ticket, subMole: SubMoleExecution, executionContext: MoleExecutionContext) = {
    val moleExecution = subMole.moleExecution
    val mole = moleExecution.mole
    def explored = ExplorationTask.explored(start)
    val (factors, outputs) = start.outputs(mole, moleExecution.sources, moleExecution.hooks).partition(explored)

    val typedFactors = factors.map(_.asInstanceOf[Val[Array[Any]]])
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

      import executionContext._

      if (condition().from(variables)) { ITransition.submitNextJobsIfReady(this)(ListBuffer() ++ variables, newTicket, subMole) }
    }

  }

  private def registerAggregationTransitions(ticket: Ticket, subMoleExecution: SubMoleExecution, executionContext: MoleExecutionContext) = {
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
            if (level > 0) toProcess += t.end.capsule → (level - 1)
            else if (level == 0) {
              subMoleExecution.aggregationTransitionRegistry.register(t, ticket, new ListBuffer)
              subMoleExecution listen {
                case (se, ev: SubMoleExecution.Finished) ⇒ t.aggregate(se, ev.ticket, executionContext)
              }
            }
          case t: IExplorationTransition ⇒ toProcess += t.end.capsule → (level + 1)
          case t                         ⇒ toProcess += t.end.capsule → level
        }
      }
    }
  }

  override def toString = s"$start -< $end"

}
