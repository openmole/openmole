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


import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.exception.MultipleException
import org.openmole.misc.exception.UserBadDataError
import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.core.model.data.IContext
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.job.MoleJobId
import org.openmole.core.model.job.ITicket

object IMoleExecution {
  final val Starting = "Starting"
  final val Finished = "Finished"
  final val OneJobStatusChanged = "OneJobStatusChanged"
  final val OneJobSubmitted = "OneJobSubmitted"
  final val JobInCapsuleFinished = "JobInCapsuleFinished"
}

trait IMoleExecution {

  @throws(classOf[InternalProcessingError])
  @throws(classOf[UserBadDataError])
  def start: this.type
    
  @throws(classOf[InternalProcessingError])
  @throws(classOf[UserBadDataError])
  def cancel: this.type
    
  @throws(classOf[InterruptedException])
  @throws(classOf[MultipleException])
  def waitUntilEnded: this.type
      
  def exceptions: Iterable[Throwable]
  
  def isFinished: Boolean

  def submit(capsule: IGenericCapsule, context: IContext, ticket: ITicket, subMole: ISubMoleExecution)

  def mole: IMole

  def rootTicket: ITicket
  def nextTicket(parent: ITicket): ITicket
  def register(subMoleExecution: ISubMoleExecution)

  def nextJobId: MoleJobId

  def localCommunication: ILocalCommunication

  def subMoleExecution(job: IMoleJob): Option[ISubMoleExecution]
    
  def ticket(job: IMoleJob): Option[ITicket]
    
  def moleJobs: Iterable[IMoleJob]
}
