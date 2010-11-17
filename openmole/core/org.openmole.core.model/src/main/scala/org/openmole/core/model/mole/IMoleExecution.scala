/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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

package org.openmole.core.model.mole


import org.openmole.commons.exception.InternalProcessingError
import org.openmole.commons.exception.UserBadDataError
import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.core.model.data.IContext
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.job.IMoleJobId
import org.openmole.core.model.job.ITicket

object IMoleExecution {
    final val Starting = "Starting"
    final val Finished = "Finished"
    final val OneJobSubmitted = "OneJobSubmitted"
    final val OneJobFinished = "OneJobFinished"
}

trait IMoleExecution {

    @throws(classOf[InternalProcessingError])
    @throws(classOf[UserBadDataError])
    def start
    
    @throws(classOf[InternalProcessingError])
    @throws(classOf[UserBadDataError])
    def cancel
    
    @throws(classOf[InterruptedException])
    def waitUntilEnded
    def isFinished: Boolean

    def submit(capsule: IGenericCapsule, global: IContext, context: IContext, ticket: ITicket, subMole: ISubMoleExecution)

    def mole: IMole

    def rootTicket: ITicket
    def nextTicket(parent: ITicket): ITicket
    def register(subMoleExecution: ISubMoleExecution)

    def nextJobId: IMoleJobId

    def localCommunication: ILocalCommunication

    def subMoleExecution(job: IMoleJob): Option[ISubMoleExecution]
    
    def ticket(job: IMoleJob): Option[ITicket]
    
    def moleJobs: Iterable[IMoleJob]
}
