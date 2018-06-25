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

import org.openmole.core.context.{ Context, Val, ValType, Variable }
import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.expansion.Condition
import org.openmole.core.fileservice.FileService
import org.openmole.core.workflow.dsl
import org.openmole.core.workflow.mole.MoleExecution.{ AggregationTransitionRegistryRecord, SubMoleExecutionState }
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.validation._
import org.openmole.tool.lock._
import org.openmole.tool.random.RandomProvider

import scala.collection.mutable
import scala.collection.mutable.{ HashSet, ListBuffer }

object AggregationTransition {

  def aggregatedOutputs(moleExecution: MoleExecution, transition: IAggregationTransition) = transition.start.outputs(moleExecution.mole, moleExecution.sources, moleExecution.hooks).toVector

  def aggregateOutputs(moleExecution: MoleExecution, transition: IAggregationTransition, results: AggregationTransitionRegistryRecord): Context = {
    val vals = aggregatedOutputs(moleExecution, transition)
    val resultValues = results.values.value
    val size = resultValues.size

    def resultsArrays = (resultValues zip results.ids.value).sortBy(_._2).unzip._1.transpose

    def variables = (resultsArrays zip vals).map {
      case (values, v) ⇒
        val result = v.`type`.manifest.newArray(values.size)
        var i = 0
        for { x ← values } {
          java.lang.reflect.Array.set(result, i, x)
          i += 1
        }
        Variable.unsecure(v, result)
    }

    new mutable.WrappedArray.ofRef(variables)
  }

  def aggregate(aggregationTransition: IAggregationTransition, subMole: SubMoleExecutionState, ticket: Ticket, executionContext: MoleExecutionContext) = {
    import executionContext.services._

    if ( /*!subMole.canceled && */ !hasBeenPerformed(aggregationTransition, subMole, ticket)) {
      val results = subMole.aggregationTransitionRegistry.remove(aggregationTransition, ticket).getOrElse(throw new InternalProcessingError("No context registered for the aggregation transition"))
      val subMoleParent = subMole.parent.getOrElse(throw new InternalProcessingError("SubMole execution has no parent"))
      val aggregated = aggregateOutputs(subMole.moleExecution, aggregationTransition, results)
      if (aggregationTransition.condition.from(aggregated)) ITransition.submitNextJobsIfReady(aggregationTransition)(aggregated.values, ticket, subMoleParent)
    }
  }

  def hasBeenPerformed(aggregationTransition: IAggregationTransition, subMole: SubMoleExecutionState, ticket: Ticket): Boolean = !subMole.aggregationTransitionRegistry.isRegistred(aggregationTransition, ticket)

  def allAggregationTransitionsPerformed(aggregationTransition: IAggregationTransition, subMole: SubMoleExecutionState, ticket: Ticket) = {

    def oneAggregationTransitionNotPerformed(subMole: SubMoleExecutionState, ticket: Ticket): Boolean = {
      val mole = subMole.moleExecution.mole
      val alreadySeen = new HashSet[Capsule]
      val toProcess = new ListBuffer[(Capsule, Int)]
      toProcess += ((aggregationTransition.start, 0))

      while (!toProcess.isEmpty) {
        val (capsule, level) = toProcess.remove(0)

        if (!alreadySeen(capsule)) {
          alreadySeen += capsule
          mole.slots(capsule).toList.flatMap { mole.inputTransitions }.foreach {
            case t: IExplorationTransition ⇒ if (level > 0) toProcess += ((t.start, level - 1))
            case t: IAggregationTransition ⇒
              if (level == 0 && t != aggregationTransition && !hasBeenPerformed(t, subMole, ticket)) return true
              toProcess += ((t.start, level + 1))
            case t ⇒ toProcess += ((t.start, level))
          }
          mole.outputTransitions(capsule).foreach {
            case t: IExplorationTransition ⇒ toProcess += ((t.end.capsule, level + 1))
            case t: IAggregationTransition ⇒
              if (level == 0 && t != aggregationTransition && !hasBeenPerformed(t, subMole, ticket)) return true
              if (level > 0) toProcess += ((t.end.capsule, level - 1))
            case t ⇒ toProcess += ((t.end.capsule, level))
          }
        }
      }
      false
    }

    !oneAggregationTransitionNotPerformed(subMole, ticket)
  }

}

class AggregationTransition(val start: Capsule, val end: Slot, val condition: Condition = Condition.True, val filter: BlockList = BlockList.empty, val trigger: Condition = Condition.False) extends IAggregationTransition with ValidateTransition {

  override def validate(inputs: Seq[Val[_]]) = Validate { p ⇒
    import p._
    condition.validate(inputs) ++ trigger.validate(inputs)
  }

  override def perform(context: Context, ticket: Ticket, moleExecution: MoleExecution, subMole: SubMoleExecution, executionContext: MoleExecutionContext) = MoleExecutionMessage.send(moleExecution) {
    MoleExecutionMessage.PerformTransition(subMole) { subMoleState ⇒
      import executionContext.services._
      val moleExecution = subMoleState.moleExecution
      val mole = moleExecution.mole
      val parentTicket = ticket.parent.getOrElse(throw new UserBadDataError("Aggregation transition should take place after an exploration."))

      if ( /*!subMole.canceled && */ !AggregationTransition.hasBeenPerformed(this, subMoleState, parentTicket)) {
        subMoleState.aggregationTransitionRegistry.consult(this, parentTicket) match {
          case Some(results) ⇒
            results.ids.append(ticket.content)
            results.values.append(AggregationTransition.aggregatedOutputs(moleExecution, this).map(v ⇒ context(v)).toArray)

            if (trigger != Condition.False) {
              val context = AggregationTransition.aggregateOutputs(moleExecution, this, results)
              if (trigger.from(context)) {
                val parentTicket = ticket.parent.getOrElse(throw new UserBadDataError("Aggregation transition should take place after an exploration"))
                val subMoleParent = subMoleState.parent.getOrElse(throw new InternalProcessingError("SubMoleExecution has no parent"))
                AggregationTransition.aggregate(this, subMoleParent, parentTicket, executionContext)
                if (AggregationTransition.allAggregationTransitionsPerformed(this, subMoleState, parentTicket)) MoleExecution.cancel(subMoleState)
              }
            }

          case None ⇒ throw new InternalProcessingError("No context registered for aggregation.")
        }
      }
    }
  }

  override def toString = s"$start >- $end"
}
