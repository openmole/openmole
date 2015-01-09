/*
 * Copyright (C) 2012 reuillon
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

import org.openmole.core.workflow.data._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.tools._

import scala.collection.mutable.ListBuffer

class EmptyExplorationTransition(start: Capsule, end: Slot, size: FromContext[Int], condition: Condition = Condition.True, filter: Filter[String] = Filter.empty) extends ExplorationTransition(start, end, condition, filter) {

  override def submitIn(context: Context, ticket: Ticket, subMole: SubMoleExecution) =
    for (i ← 0 until size.from(context)) submitNextJobsIfReady(ListBuffer() ++ filtered(context).values, subMole.moleExecution.nextTicket(ticket), subMole)

}
