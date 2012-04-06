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
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import org.openmole.core.implementation.data.Context
import org.openmole.core.model.mole.IAtomicCapsule
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.implementation.validation.Validation
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.job.IJob
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.job.IMoleJob.moleJobOrdering
import org.openmole.core.model.job.MoleJobId
import org.openmole.core.model.mole.IEnvironmentSelection
import org.openmole.core.model.mole.IMole
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.exception.MultipleException
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.eventdispatcher.Event
import org.openmole.misc.eventdispatcher.EventListener
import org.openmole.core.model.mole.ITicket
import org.openmole.core.model.mole.IMoleJobGroup
import org.openmole.core.model.mole.IMoleJobGrouping
import org.openmole.core.model.mole.ISubMoleExecution
import org.openmole.core.model.data.IDataChannel
import org.openmole.misc.tools.service.Priority
import org.openmole.core.implementation.execution.local.LocalExecutionEnvironment
import org.openmole.core.implementation.job.Job
import org.openmole.core.implementation.tools.RegistryWithTicket
import org.openmole.core.model.mole.IGroupingStrategy
import org.openmole.core.model.mole.IInstantRerun
import scala.collection.mutable.Buffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._

object MoleExecution extends Logger

class MoleExecution(val mole: IMole, val environmentSelection: IEnvironmentSelection, moleJobGrouping: IMoleJobGrouping, _instantRerun: IInstantRerun) extends IMoleExecution {

  def this(mole: IMole) = this(mole, FixedEnvironmentSelection.Empty, MoleJobGrouping.Empty, IInstantRerun.empty)
    
  def this(mole: IMole, environmentSelection: IEnvironmentSelection) = this(mole, environmentSelection, MoleJobGrouping.Empty, IInstantRerun.empty)
      
  def this(mole: IMole, environmentSelection: IEnvironmentSelection, moleJobGrouping: IMoleJobGrouping) = this(mole, environmentSelection, moleJobGrouping, IInstantRerun.empty)
  
  def this(mole: IMole, instantRerun: IInstantRerun) = this(mole, FixedEnvironmentSelection.Empty, MoleJobGrouping.Empty, instantRerun)
    
  def this(mole: IMole, environmentSelection: IEnvironmentSelection, instantRerun: IInstantRerun) = this(mole, environmentSelection, MoleJobGrouping.Empty, instantRerun)

  import IMoleExecution._
  import MoleExecution._
 
  private val _started = new AtomicBoolean(false)
  private val canceled = new AtomicBoolean(false)
  private val _finished = new Semaphore(0)

  override val id = UUID.randomUUID.toString  
  private val ticketNumber = new AtomicLong
  private val currentJobId = new AtomicLong

  private val waitingJobs = new HashMap[(ICapsule, IMoleJobGroup), ListBuffer[IMoleJob]]
  private var nbWaiting = 0
  
  
  val rootSubMoleExecution = new SubMoleExecution(None, this)
  val rootTicket = Ticket(id, ticketNumber.getAndIncrement)  
  val dataChannelRegistry = new RegistryWithTicket[IDataChannel, Buffer[IVariable[_]]]

  val exceptions = new ListBuffer[Throwable]
  

  def instantRerun(moleJob: IMoleJob, capsule: ICapsule) = synchronized {
    _instantRerun.rerun(moleJob, capsule)
  }
  
  def group(moleJob: IMoleJob, capsule: ICapsule) = synchronized {
    moleJobGrouping(capsule) match {
      case Some(strategy) =>
        val (category, complete) = strategy.group(moleJob.context)
        val key = (capsule, category)
            
        waitingJobs.getOrElseUpdate(key, new ListBuffer) += moleJob 
        nbWaiting += 1
            
        if(complete) {
          val toSubmit = waitingJobs.remove(key).getOrElse(new ListBuffer)
          nbWaiting -= toSubmit.size
          val job = new Job(id, toSubmit)
          submit(job, capsule)
        }
      case None =>
        val job = new Job(id, List(moleJob))
        submit(job, capsule)
    }
    
  }
  
  def submit(job: IJob, capsule: ICapsule) = 
    capsule match {
      case _: IAtomicCapsule => synchronized { job.moleJobs.foreach{_.perform} }
      case _ =>
        (environmentSelection.select(capsule) match {
            case Some(environment) => environment
            case None => LocalExecutionEnvironment
          }).submit(job)
    }
  
  
  def submitAll = {
    waitingJobs.foreach {
      case((capsule, _), jobs) => submit(new Job(id, jobs) , capsule)
    }
    nbWaiting = 0
    waitingJobs.empty
  }
 
  def start(context: IContext): this.type = {
    rootSubMoleExecution.newChild.submit(mole.root, context, nextTicket(rootTicket))
    if(rootSubMoleExecution.nbJobInProgress <= nbWaiting) submitAll
    this
  }
  
  override def start = {
    if(!_started.getAndSet(true)) {
      val validationErrors = Validation(mole)
      if(!validationErrors.isEmpty) throw new UserBadDataError("Formal validation of you mole has failed, several errors have been found: " + validationErrors.mkString("; "))
      start(Context.empty) 
    }
    this
  }
  
  override def cancel: this.type = {
    if(!canceled.getAndSet(true)) {
      rootSubMoleExecution.cancel
      EventDispatcher.trigger(this, new IMoleExecution.Finished)
    }
    this
  }

  override def moleJobs = rootSubMoleExecution.jobs

  override def waitUntilEnded = {
    _finished.acquire
    _finished.release
    if(!exceptions.isEmpty) throw new MultipleException(exceptions)
    this
  }
    
  def jobFailedOrCanceled(moleJob: IMoleJob, capsule: ICapsule) = synchronized {
    moleJob.exception match {
      case None =>
      case Some(e) => exceptions += e
    }
    jobOutputTransitionsPerformed(moleJob, capsule)
  }

  def jobOutputTransitionsPerformed(job: IMoleJob, capsule: ICapsule) = synchronized {
    if(rootSubMoleExecution.nbJobInProgress <= nbWaiting) submitAll
    if(!canceled.get)  {
      _instantRerun.jobFinished(job, capsule)
      if (finished) {  
        _finished.release
        EventDispatcher.trigger(this, new IMoleExecution.Finished)
      }
    }
  }
  
  override def finished: Boolean = rootSubMoleExecution.nbJobInProgress == 0
  
  override def started: Boolean = _started.get

  override def nextTicket(parent: ITicket): ITicket = Ticket(parent, ticketNumber.getAndIncrement)

  def nextJobId = new MoleJobId(id, currentJobId.getAndIncrement)
 
  
}
