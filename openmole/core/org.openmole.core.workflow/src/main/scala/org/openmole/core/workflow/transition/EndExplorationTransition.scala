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

import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.tools.Condition
import Condition._
import scala.util.{ Random, Failure, Success, Try }
import org.openmole.tool.lock._
import org.openmole.core.workflow.dsl._

class EndExplorationTransition(val start: Capsule, val end: Slot, val trigger: Condition, val filter: BlockList = BlockList.empty) extends IEndExplorationTransition {

  //def condition = Condition.True

  override def perform(context: Context, ticket: Ticket, subMole: SubMoleExecution)(implicit rng: RandomProvider) = {
    def perform() {
      val parentTicket = ticket.parent.getOrElse(throw new UserBadDataError("End exploration transition should take place after an exploration."))
      val subMoleParent = subMole.parent.getOrElse(throw new InternalProcessingError("Submole execution has no parent"))
      subMoleParent.transitionLock { submitNextJobsIfReady(context.values, parentTicket, subMoleParent) }
      subMole.cancel
    }

    Try(!subMole.canceled && trigger.from(context)) match {
      case Success(true) ⇒ perform()
      case Failure(t) ⇒
        subMole.cancel; throw t
      case _ ⇒
    }
  }

}
