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
import org.openmole.core.fileservice.FileService
import org.openmole.core.workflow.dsl
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.mole.MoleExecution.{ AggregationTransitionRegistryRecord, SubMoleExecutionState }
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.validation._
import org.openmole.core.workspace.NewFile
import org.openmole.tool.lock._

import scala.collection.mutable.{ HashSet, ListBuffer }

object ExplorationTransition {

  def registerAggregationTransitions(transition: ExplorationTransition, ticket: Ticket, subMoleExecution: SubMoleExecutionState, executionContext: MoleExecutionContext, size: Int) = {
    val alreadySeen = new HashSet[MoleCapsule]
    val toProcess = new ListBuffer[(MoleCapsule, Int)]
    toProcess += ((transition.end.capsule, 0))
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
              subMoleExecution.aggregationTransitionRegistry.register(t, ticket, AggregationTransitionRegistryRecord(size))
              subMoleExecution.onFinish += { se ⇒ AggregationTransition.aggregate(t, se, ticket, executionContext) }
            }
          case t: IExplorationTransition ⇒ toProcess += t.end.capsule → (level + 1)
          case t                         ⇒ toProcess += t.end.capsule → level
        }
      }
    }
  }

  def factors(transition: ExplorationTransition, moleExecution: MoleExecution) = {
    def explored = ExplorationTask.explored(transition.start)
    transition.start.outputs(moleExecution.mole, moleExecution.sources, moleExecution.hooks).partition(explored)
    val (factors, outputs) = transition.start.outputs(moleExecution.mole, moleExecution.sources, moleExecution.hooks).partition(explored)
    val typedFactors = factors.map(_.asInstanceOf[Val[Array[Any]]])
    (typedFactors, outputs)
  }

  def exploredSamples(transition: ExplorationTransition, context: Context, moleExecution: MoleExecution) = {
    def values = factors(transition, moleExecution)._1.toArray.map(context(_).toArray).transpose
    values
  }

  def submitIn(transition: ExplorationTransition, context: Context, ticket: Ticket, samples: Array[Array[Any]], subMole: SubMoleExecutionState, executionContext: MoleExecutionContext) = {
    val moleExecution = subMole.moleExecution
    val mole = moleExecution.mole
    val (typedFactors, outputs) = factors(transition, moleExecution)

    for (value ← samples) {
      val newTicket = MoleExecution.nextTicket(moleExecution, ticket)
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

      import executionContext.services._

      if (transition.condition.from(variables)) { ITransition.submitNextJobsIfReady(transition)(ListBuffer() ++ variables, newTicket, subMole) }
    }

  }

}

class ExplorationTransition(val start: MoleCapsule, val end: TransitionSlot, val condition: Condition = Condition.True, val filter: BlockList = BlockList.empty) extends IExplorationTransition with ValidateTransition {

  override def validate(inputs: Seq[Val[_]]) = Validate { p ⇒
    import p._
    condition.validate(inputs)
  }

  override def perform(context: Context, ticket: Ticket, moleExecution: MoleExecution, subMole: SubMoleExecution, executionContext: MoleExecutionContext) = MoleExecutionMessage.send(moleExecution) {
    MoleExecutionMessage.PerformTransition(subMole) { subMoleState ⇒
      val subSubMole = MoleExecution.newChildSubMoleExecution(subMoleState)
      val samples = ExplorationTransition.exploredSamples(this, context, moleExecution)
      ExplorationTransition.registerAggregationTransitions(this, ticket, subSubMole, executionContext, samples.size)
      ExplorationTransition.submitIn(this, filtered(context), ticket, samples, subSubMole, executionContext)
    }
  }

  override def toString = s"$start -< $end"

}
