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

package org.openmole.core.implementation.mole

import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import org.openmole.core.implementation.data.Context
import org.openmole.core.model.job.State
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.execution.IEnvironment
import org.openmole.core.model.job.IJob
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.job.MoleJobId
import org.openmole.core.model.mole.IEnvironmentSelection
import org.openmole.core.model.mole.IMole
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.exception.MultipleException
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.eventdispatcher.Event
import org.openmole.misc.eventdispatcher.EventListener
import org.openmole.core.model.mole.ITicket
import org.openmole.core.model.job.State.State
import org.openmole.core.model.mole.IMoleJobGroup
import org.openmole.core.model.mole.IMoleJobGrouping
import org.openmole.core.model.mole.ISubMoleExecution
import org.openmole.core.model.data.IDataChannel
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.tools.service.Priority
import org.openmole.core.implementation.execution.local.LocalExecutionEnvironment
import org.openmole.core.implementation.tools.RegistryWithTicket
import org.openmole.core.model.mole.IInstantRerun
import scala.collection.immutable.TreeMap
import scala.collection.mutable.Buffer
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._

object MoleExecution extends Logger

class MoleExecution(val mole: IMole, environmentSelection: IEnvironmentSelection, moleJobGrouping: IMoleJobGrouping, instantRerun: IInstantRerun) extends IMoleExecution {

  def this(mole: IMole) = this(mole, FixedEnvironmentSelection.Empty, MoleJobGrouping.Empty, IInstantRerun.empty)
    
  def this(mole: IMole, environmentSelection: IEnvironmentSelection) = this(mole, environmentSelection, MoleJobGrouping.Empty, IInstantRerun.empty)
      
  def this(mole: IMole, environmentSelection: IEnvironmentSelection, moleJobGrouping: IMoleJobGrouping) = this(mole, environmentSelection, moleJobGrouping, IInstantRerun.empty)
  
  def this(mole: IMole, instantRerun: IInstantRerun) = this(mole, FixedEnvironmentSelection.Empty, MoleJobGrouping.Empty, instantRerun)
    
  def this(mole: IMole, environmentSelection: IEnvironmentSelection, instantRerun: IInstantRerun) = this(mole, environmentSelection, MoleJobGrouping.Empty, instantRerun)

  import IMoleExecution._
  import MoleExecution._

  @transient lazy private val moleExecutionAdapterForMoleJob = new EventListener[IMoleJob] {
    override def triggered(job: IMoleJob, ev: Event[IMoleJob]) = 
      ev match {
        case ev: IMoleJob.StateChanged => EventDispatcher.trigger(MoleExecution.this, new IMoleExecution.OneJobStatusChanged(job, ev.newState, ev.oldState))
        case ev: IMoleJob.TransitionPerformed => MoleExecution.this.jobOutputTransitionsPerformed(job, ev.capsule)
        case ev: IMoleJob.JobFailedOrCanceled => MoleExecution.this.jobFailedOrCanceled(job, ev.capsule)
      }
  }
 
  private var inProgress = new TreeMap[IMoleJob, ITicket] //with SynchronizedMap[IMoleJob, (ISubMoleExecution, ITicket)] 
  private val started = new AtomicBoolean(false)
  private val canceled = new AtomicBoolean(false)
  private val finished = new Semaphore(0)

  override val id = UUID.randomUUID.toString  
  private val ticketNumber = new AtomicLong
  private val currentJobId = new AtomicLong

  val rootTicket = Ticket(id, ticketNumber.getAndIncrement)  
  val dataChannelRegistry = new RegistryWithTicket[IDataChannel, Buffer[IVariable[_]]]

  val exceptions = new ListBuffer[Throwable]
  

  override def submit(moleJob: IMoleJob, capsule: ICapsule, subMole: ISubMoleExecution, ticket: ITicket): Unit = synchronized {
    if(!canceled.get)  {
      EventDispatcher.trigger(this, new JobInCapsuleStarting(moleJob, capsule))
        
      EventDispatcher.trigger(this, new IMoleExecution.OneJobSubmitted(moleJob))
    
      EventDispatcher.listen(moleJob, Priority.HIGH, moleExecutionAdapterForMoleJob, classOf[IMoleJob.StateChanged])
      EventDispatcher.listen(moleJob, Priority.NORMAL, moleExecutionAdapterForMoleJob, classOf[IMoleJob.TransitionPerformed])
      EventDispatcher.listen(moleJob, Priority.NORMAL, moleExecutionAdapterForMoleJob, classOf[IMoleJob.JobFailedOrCanceled])

      inProgress += moleJob -> ticket

      if(!instantRerun.rerun(moleJob, capsule)) subMole.group(moleJob, capsule, moleJobGrouping(capsule))
    }
  }

  override def submitToEnvironment(job: IJob, capsule: ICapsule): Unit = {
    (environmentSelection.select(capsule) match {
        case Some(environment) => environment
        case None => LocalExecutionEnvironment
      }).submit(job)
  }
 
  def start(context: IContext): this.type = {
    val ticket = nextTicket(rootTicket)
    val subMole = SubMoleExecution(this)
    //val moleJob = mole.root.toJob(context, nextJobId, subMole, ticket)
      
    subMole.submit(mole.root, context, ticket)
    this
  }
  
  override def start = {
    if(!started.getAndSet(true)) start(Context.empty) 
    this
  }
  
  override def cancel: this.type = {
    if(!canceled.getAndSet(true)) {
      for (moleJob <- inProgress.keySet) moleJob.cancel
      inProgress = TreeMap.empty
      EventDispatcher.trigger(this, new IMoleExecution.Finished)
    }
    this
  }

  override def moleJobs = inProgress.keys

  override def waitUntilEnded = {
    finished.acquire
    finished.release
    if(!exceptions.isEmpty) throw new MultipleException(exceptions)
    this
  }
    
  private def jobFailedOrCanceled(moleJob: IMoleJob, capsule: ICapsule) = synchronized {
    moleJob.exception match {
      case None =>
      case Some(e) => exceptions += e
    }
    jobOutputTransitionsPerformed(moleJob, capsule)
  }

  private def jobOutputTransitionsPerformed(job: IMoleJob, capsule: ICapsule) = synchronized {
    //val (subMole, ticket) = inProgress.getOrElse(job, throw new InternalProcessingError("Error in mole execution job info not found"))
    if(!canceled.get)  {
      inProgress -= job
      instantRerun.jobFinished(job, capsule)
     
      if (isFinished) {  
        finished.release
        EventDispatcher.trigger(this, new IMoleExecution.Finished)
      }
    }
  }
  
  override def isFinished: Boolean = inProgress.isEmpty

  override def nextTicket(parent: ITicket): ITicket = Ticket(parent, ticketNumber.getAndIncrement)

  override def nextJobId = new MoleJobId(id, currentJobId.getAndIncrement)
        
  def ticket(job: IMoleJob) =
    inProgress.getOrElse(job, throw new InternalProcessingError("Error in mole execution job info not found"))

}
