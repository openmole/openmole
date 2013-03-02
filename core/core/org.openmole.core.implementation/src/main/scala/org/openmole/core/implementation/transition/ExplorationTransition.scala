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

package org.openmole.core.implementation.transition

import org.openmole.core.implementation.task.ExplorationTask._
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.mole._
import org.openmole.core.model.task._
import org.openmole.core.implementation.tools._
import org.openmole.core.model.data.DataModeMask._
import org.openmole.core.model.tools._
import org.openmole.core.model.transition._
import org.openmole.core.model.data._
import org.openmole.core.model.mole._
import org.openmole.misc.eventdispatcher._
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.tools.obj.ClassUtils._
import org.openmole.misc.tools.service.Priority
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer
import org.openmole.misc.tools.service.LockUtil._

class ExplorationTransition(start: ICapsule, end: Slot, condition: ICondition = ICondition.True, filter: Filter[String] = Filter.empty) extends Transition(start, end, condition, filter) with IExplorationTransition {

  override def _perform(context: Context, ticket: ITicket, subMole: ISubMoleExecution) = {
    val subSubMole = subMole.newChild

    registerAggregationTransitions(ticket, subSubMole)
    subSubMole.transitionLock { submitIn(context, ticket, subSubMole) }
  }

  def submitIn(context: Context, ticket: ITicket, subMole: ISubMoleExecution) = {
    val moleExecution = subMole.moleExecution
    val mole = moleExecution.mole
    val (factors, outputs) = start.outputs(mole, moleExecution.sources, moleExecution.hooks).partition(d ⇒ (d.mode is Explore) && d.prototype.`type`.isArray)
    val typedFactors = factors.map(_.prototype.asInstanceOf[Prototype[Array[Any]]])
    val values = typedFactors.toList.map(context(_).toIterable).transpose

    for (value ← values) {
      val newTicket = subMole.moleExecution.nextTicket(ticket)
      val variables = new ListBuffer[Variable[_]]

      for (in ← outputs)
        context.variable(in.prototype) match {
          case Some(v) ⇒ variables += v
          case None ⇒
        }

      for ((p, v) ← typedFactors zip value) {
        val fp = p.fromArray
        if (fp.accepts(v)) variables += Variable(fp, v)
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

        subMoleExecution.moleExecution.mole.outputTransitions(capsule).foreach {
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
