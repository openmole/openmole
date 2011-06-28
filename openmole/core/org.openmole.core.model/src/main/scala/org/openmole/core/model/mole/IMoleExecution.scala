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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.model.mole


import org.openmole.core.model.tools.IContextBuffer
import org.openmole.core.model.tools.IRegistryWithTicket
import org.openmole.core.model.transition.IGenericTransition
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.exception.MultipleException
import org.openmole.misc.exception.UserBadDataError
import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IDataChannel
import org.openmole.core.model.job.IJob
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.job.MoleJobId

object IMoleExecution {
  final val Starting = "Starting"
  final val Finished = "Finished"
  final val OneJobStatusChanged = "OneJobStatusChanged"
  final val OneJobSubmitted = "OneJobSubmitted"
  final val JobInCapsuleFinished = "JobInCapsuleFinished"
  final val JobInCapsuleStarting = "JobInCapsuleStarting"
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

  def submit(moleJob: IMoleJob, capsule: IGenericCapsule, subMole: ISubMoleExecution, ticket: ITicket)
  def submitToEnvironment(job: IJob, capsule: IGenericCapsule)

  def mole: IMole

  def rootTicket: ITicket
  def nextTicket(parent: ITicket): ITicket
  
  def nextJobId: MoleJobId
  
  def dataChannelRegistry: IRegistryWithTicket[IDataChannel, IContextBuffer]
  def subMoleExecution(job: IMoleJob): Option[ISubMoleExecution]
      
  def ticket(job: IMoleJob): Option[ITicket]
    
  def moleJobs: Iterable[IMoleJob]
}
