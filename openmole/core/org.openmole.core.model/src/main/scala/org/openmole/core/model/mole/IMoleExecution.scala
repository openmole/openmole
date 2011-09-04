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


import org.openmole.core.model.job.State.State
import org.openmole.core.model.tools.IVariablesBuffer
import org.openmole.core.model.tools.IRegistryWithTicket
import org.openmole.misc.eventdispatcher.{IObjectListener, Event}
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.exception.MultipleException
import org.openmole.misc.exception.UserBadDataError
import org.openmole.core.model.data.IDataChannel
import org.openmole.core.model.job.IJob
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.job.MoleJobId

object IMoleExecution {
  
  trait IStarting extends IObjectListener[IMoleExecution] {
    override def eventOccured(obj: IMoleExecution, args: Array[Object]) = starting(obj)
    
    def starting(moleJob: IMoleExecution)
  }
 
  trait IFinished extends IObjectListener[IMoleExecution] {
    override def eventOccured(obj: IMoleExecution, args: Array[Object]) = finished(obj)
    
    def finished(moleJob: IMoleExecution)
  }
  
  trait IOneJobStatusChanged extends IObjectListener[IMoleExecution] {
    override def eventOccured(obj: IMoleExecution, args: Array[Object]) = 
      oneJobStatusChanged(obj, args(0).asInstanceOf[IMoleJob], args(1).asInstanceOf[State], args(2).asInstanceOf[State])
    
    def oneJobStatusChanged(execution: IMoleExecution, moleJob: IMoleJob, newState: State, oldState: State)
  }
  
  trait IOneJobSubmitted extends IObjectListener[IMoleExecution] {
    override def eventOccured(obj: IMoleExecution, args: Array[Object]) = oneJobSubmitted(obj, args(0).asInstanceOf[IMoleJob])
    
    def oneJobSubmitted(execution: IMoleExecution, moleJob: IMoleJob)
  }
  
  trait IJobInCapsuleFinished extends IObjectListener[IMoleExecution] {
    override def eventOccured(obj: IMoleExecution, args: Array[Object]) = 
      jobInCapsuleFinished(obj, args(0).asInstanceOf[IMoleJob], args(1).asInstanceOf[ICapsule])
    
    def jobInCapsuleFinished(execution: IMoleExecution, moleJob: IMoleJob, capsule: ICapsule)
  }
  
  trait IJobInCapsuleStarting extends IObjectListener[IMoleExecution] {
    override def eventOccured(obj: IMoleExecution, args: Array[Object]) = 
      jobInCapsuleStarting(obj, args(0).asInstanceOf[IMoleJob], args(1).asInstanceOf[ICapsule])
    
    def jobInCapsuleStarting(execution: IMoleExecution, moleJob: IMoleJob, capsule: ICapsule)
  }
  
  final val Starting = new Event[IMoleExecution, IStarting]
  final val Finished = new Event[IMoleExecution, IFinished]
  final val OneJobStatusChanged = new Event[IMoleExecution, IOneJobStatusChanged]
  final val OneJobSubmitted = new Event[IMoleExecution, IOneJobSubmitted]
  final val JobInCapsuleFinished = new Event[IMoleExecution, IJobInCapsuleFinished]
  final val JobInCapsuleStarting = new Event[IMoleExecution, IJobInCapsuleStarting]
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

  def submit(moleJob: IMoleJob, capsule: ICapsule, subMole: ISubMoleExecution, ticket: ITicket)
  def submitToEnvironment(job: IJob, capsule: ICapsule)

  def mole: IMole

  def rootTicket: ITicket
  def nextTicket(parent: ITicket): ITicket
  
  def nextJobId: MoleJobId
  
  def dataChannelRegistry: IRegistryWithTicket[IDataChannel, IVariablesBuffer]
  def subMoleExecution(job: IMoleJob): Option[ISubMoleExecution]
      
  def ticket(job: IMoleJob): Option[ITicket]
    
  def moleJobs: Iterable[IMoleJob]
  def id: String
}
