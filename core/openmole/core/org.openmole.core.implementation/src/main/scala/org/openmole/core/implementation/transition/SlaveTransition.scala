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

import org.openmole.core.implementation.tools._
import org.openmole.core.model.data._
import org.openmole.core.model.mole._
import org.openmole.core.model.tools._
import org.openmole.core.model.transition._
import org.openmole.misc.exception._

class SlaveTransition(start: ICapsule, end: Slot, condition: ICondition = ICondition.True, filter: Filter[String] = Filter.empty) extends ExplorationTransition(start, end, condition, filter) with ISlaveTransition {

  override def _perform(context: Context, ticket: ITicket, subMole: ISubMoleExecution) =
    submitIn(context, ticket.parent.getOrElse(throw new UserBadDataError("Slave transition should take place after an master transition.")), subMole)

}
