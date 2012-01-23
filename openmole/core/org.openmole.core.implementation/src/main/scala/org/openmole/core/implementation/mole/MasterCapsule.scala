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

package org.openmole.core.implementation.mole

import org.openmole.core.model.mole.ISubMoleExecution
import org.openmole.core.model.mole.ITicket
import org.openmole.core.model.task.ITask
import org.openmole.core.implementation.data.Context
import org.openmole.core.model.data.IContext
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.job.MoleJobId
import org.openmole.core.model.mole.IMasterCapsule
import org.openmole.misc.exception.UserBadDataError

class MasterCapsule(_task: Option[ITask] = None) extends Capsule(_task) with IMasterCapsule {
  
  def this(t: ITask) = this(Some(t))
  
  override def toJob(context: IContext, jobId: MoleJobId, subMoleExecution: ISubMoleExecution, ticket: ITicket): IMoleJob = {
    val parentTicket = ticket.parent.getOrElse(throw new UserBadDataError("Parrent ticket is not avialable."))
    val savedContext = subMoleExecution.masterCapsuleRegistry.remove(this, parentTicket).getOrElse(new Context)
    super.toJob(savedContext ++ context, jobId, subMoleExecution, ticket)
  }

  override protected def performTransition(context: IContext, ticket: ITicket, subMole: ISubMoleExecution) = {   
    val parentTicket = ticket.parent.getOrElse(throw new UserBadDataError("Parrent ticket is not avialable."))
    subMole.masterCapsuleRegistry.register(this, parentTicket, context)
    super.performTransition(context, ticket, subMole)
  }
  
}