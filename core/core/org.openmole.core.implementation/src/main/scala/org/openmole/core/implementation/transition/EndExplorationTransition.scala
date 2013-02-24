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

package org.openmole.core.implementation.transition

import org.openmole.core.implementation.tools._
import org.openmole.core.model.tools._
import org.openmole.core.model.data._
import org.openmole.core.model.mole._
import org.openmole.core.model.transition.ICondition._
import org.openmole.core.model.transition._
import org.openmole.misc.exception._
import org.openmole.misc.tools.service.LockUtil._

class EndExplorationTransition(start: ICapsule, end: Slot, trigger: ICondition, filter: Filter[String] = Filter.empty) extends Transition(start, end, True, filter) with IEndExplorationTransition {

  override protected def _perform(context: Context, ticket: ITicket, subMole: ISubMoleExecution) = {
    if (!subMole.canceled && trigger.evaluate(context)) {
      val parentTicket = ticket.parent.getOrElse(throw new UserBadDataError("End exploration transition should take place after an exploration."))
      val subMoleParent = subMole.parent.getOrElse(throw new InternalProcessingError("Submole execution has no parent"))
      subMoleParent.transitionLock { super._perform(context, parentTicket, subMoleParent) }
      subMole.cancel
    }
  }

}
