/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.core.implementation.transition

import org.openmole.core.implementation.task.ExplorationTask._
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.data.Context._
import org.openmole.core.implementation.mole.SubMoleExecution
import org.openmole.core.implementation.mole.Capsule._
import org.openmole.core.model.task.IExplorationTask
import org.openmole.core.model.task.ITask
import org.openmole.core.model.data.DataModeMask._
import org.openmole.core.model.transition.{ IExplorationTransition, ISlot, ICondition, IAggregationTransition }
import org.openmole.core.model.data.{ IPrototype, IContext, IVariable, IData }
import org.openmole.core.model.mole.{ ICapsule, ITicket, ISubMoleExecution }
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.tools.obj.ClassUtils._
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.tools.service.Priority
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer

class ExplorationTransition(start: ICapsule, end: ISlot, condition: ICondition = ICondition.True, filtered: Iterable[String] = Iterable.empty[String]) extends Transition(start, end, condition, filtered) with IExplorationTransition {

  override def _perform(context: IContext, ticket: ITicket, subMole: ISubMoleExecution) = {
    val subSubMole = subMole.newChild

    registerAggregationTransitions(ticket, subSubMole)
    submitIn(context, ticket, subSubMole)
  }

  def submitIn(context: IContext, ticket: ITicket, subMole: ISubMoleExecution) = {
    val (factors, outputs) = start.outputs.partition(d ⇒ (d.mode is explore) && d.prototype.`type`.isArray)
    val typedFactors = factors.map(_.prototype.asInstanceOf[IPrototype[Array[Any]]])
    val values = typedFactors.toList.map(context.value(_).get.toIterable).transpose

    val endTask = end.capsule.taskOrException

    for (value ← values) {

      val newTicket = subMole.moleExecution.nextTicket(ticket)

      val variables = new ListBuffer[IVariable[_]]

      for (in ← outputs)
        context.variable(in.prototype) match {
          case Some(v) ⇒ variables += v
          case None ⇒
        }

      for ((p, v) ← typedFactors zip value) {
        val fp = p.fromArray
        if (fp.accepts(v)) variables += new Variable(fp, v)
        else throw new UserBadDataError("Found value of type " + v.asInstanceOf[AnyRef].getClass + " incompatible with prototype " + fp)
      }
      submitNextJobsIfReady(ListBuffer() ++ variables, newTicket, subMole)
    }

  }

  private def registerAggregationTransitions(ticket: ITicket, subMoleExecution: ISubMoleExecution) = {
    val alreadySeen = new HashSet[ICapsule]
    val toProcess = new ListBuffer[(ICapsule, Int)]
    toProcess += ((end.capsule, 0))

    while (!toProcess.isEmpty) {
      val cur = toProcess.remove(0)
      val capsule = cur._1
      val level = cur._2

      if (!alreadySeen(capsule)) {
        alreadySeen += capsule

        capsule.outputTransitions.foreach {
          case t: IAggregationTransition ⇒
            if (level > 0) toProcess += t.end.capsule -> (level - 1)
            else if (level == 0) {
              subMoleExecution.aggregationTransitionRegistry.register(t, ticket, new ListBuffer)
              EventDispatcher.listen(subMoleExecution, Priority.LOW, new AggregationTransitionAdapter(t), classOf[ISubMoleExecution.Finished])
            }
          case t: IExplorationTransition ⇒ toProcess += t.end.capsule -> (level + 1)
          case t ⇒ toProcess += t.end.capsule -> level
        }
      }
    }
  }

}
