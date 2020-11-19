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

import org.openmole.core.context.{ Context, Val }
import org.openmole.core.expansion.Condition
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.validation._
import org.openmole.tool.logger.JavaLogger
import cats.implicits._
import org.openmole.core.fileservice.FileService
import org.openmole.core.workflow.dsl
import org.openmole.core.workflow.mole.MoleExecutionMessage.PerformTransition
import org.openmole.core.workspace.TmpDirectory

object DirectTransition extends JavaLogger

/**
 * Transition between a mole and a slot
 *
 * @param start
 * @param end
 * @param condition
 * @param filter
 */
class DirectTransition(
  val start:     MoleCapsule,
  val end:       TransitionSlot,
  val condition: Condition      = Condition.True,
  val filter:    BlockList      = BlockList.empty
) extends Transition with ValidateTransition {

  override def validate(inputs: Seq[Val[_]]) = condition.validate(inputs)

  override def perform(context: Context, ticket: Ticket, moleExecution: MoleExecution, subMole: SubMoleExecution, moleExecutionContext: MoleExecutionContext) = MoleExecutionMessage.send(moleExecution) {
    PerformTransition(subMole) { subMoleState ⇒
      import moleExecutionContext.services._
      if (condition.from(context)) Transition.submitNextJobsIfReady(this)(filtered(context).values, ticket, subMoleState)
    }
  }

  override def toString = s"$start -- $end"
}
