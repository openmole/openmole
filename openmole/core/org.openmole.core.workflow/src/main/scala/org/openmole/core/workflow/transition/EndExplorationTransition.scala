/*
 * Copyright (C) 2012 Romain Reuillon
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

import org.openmole.core.context.{ Context, Val }
import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.expansion.{ Condition, FromContext }
import org.openmole.core.fileservice.FileService
import org.openmole.core.workflow.dsl
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.validation._
import org.openmole.core.workspace.NewFile
import org.openmole.tool.lock._

import scala.util.{ Failure, Success, Try }

class EndExplorationTransition(val start: MoleCapsule, val end: TransitionSlot, val trigger: Condition, val filter: BlockList = BlockList.empty) extends IEndExplorationTransition with ValidateTransition {

  override def validate(inputs: Seq[Val[_]]) = Validate { p ⇒
    import p._
    trigger.validate(inputs)
  }

  override def perform(context: Context, ticket: Ticket, moleExecution: MoleExecution, subMole: SubMoleExecution, executionContext: MoleExecutionContext) = MoleExecutionMessage.send(moleExecution) {
    MoleExecutionMessage.PerformTransition(subMole) { subMoleState ⇒
      def perform() {
        val parentTicket = ticket.parent.getOrElse(throw new UserBadDataError("End exploration transition should take place after an exploration."))
        val subMoleParent = subMoleState.parent.getOrElse(throw new InternalProcessingError("Submole execution has no parent"))
        //subMoleParent.transitionLock { ITransition.submitNextJobsIfReady(this)(context.values, parentTicket, subMoleParent) }
        ITransition.submitNextJobsIfReady(this)(context.values, parentTicket, subMoleParent)
        MoleExecution.cancel(subMoleState) //.cancel
      }

      import executionContext.services._

      Try( /*!subMoleState.canceled && */ trigger.from(context)) match {
        case Success(true)  ⇒ perform()
        case Success(false) ⇒
        case Failure(t) ⇒
          MoleExecution.cancel(subMoleState)
          throw t
      }
    }
  }

  override def toString = s"$start >| $end"
}
