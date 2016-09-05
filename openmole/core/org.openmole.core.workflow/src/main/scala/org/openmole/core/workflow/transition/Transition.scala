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

import org.openmole.core.workflow.validation._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.tools._
import org.openmole.tool.logger.Logger

import org.openmole.core.workflow.dsl._

object Transition extends Logger

class Transition(
    val start:     Capsule,
    val end:       Slot,
    val condition: Condition = Condition.True,
    val filter:    BlockList = BlockList.empty
) extends ITransition with ValidateTransition {

  override def validate(inputs: Seq[Prototype[_]]) = condition.validate(inputs)

  override def perform(context: Context, ticket: Ticket, subMole: SubMoleExecution)(implicit rng: RandomProvider) =
    if (condition().from(context)) submitNextJobsIfReady(filtered(context).values, ticket, subMole)

  override def toString = this.getClass.getSimpleName + " from " + start + " to " + end

}
