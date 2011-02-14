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

package org.openmole.core.implementation.mole

import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Level
import java.util.logging.Logger
import org.openmole.core.implementation.data.Context
import org.openmole.core.implementation.execution.JobRegistry
import org.openmole.core.implementation.internal.Activator._
import org.openmole.core.implementation.job.Job
import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.core.model.data.IContext
import org.openmole.core.model.execution.IEnvironment
import org.openmole.core.model.job.IJob
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.job.MoleJobId
import org.openmole.core.model.mole.IEnvironmentSelection
import org.openmole.core.model.mole.IMole
import org.openmole.core.model.mole.IMoleExecution
import org.apache.commons.collections15.bidimap.DualHashBidiMap
import org.apache.commons.collections15.multimap.MultiHashMap
import org.openmole.commons.aspect.eventdispatcher.IObjectListener
import org.openmole.commons.aspect.eventdispatcher.BeforeObjectModified
import org.openmole.core.model.job.ITicket
import org.openmole.core.model.job.State
import org.openmole.core.model.mole.IMoleJobGroup
import org.openmole.core.model.mole.IMoleJobGrouping
import org.openmole.core.model.mole.ISubMoleExecution
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.commons.tools.service.Priority
import org.openmole.core.implementation.execution.local.LocalExecutionEnvironment
import scala.collection.immutable.TreeMap
import scala.collection.JavaConversions._

object MoleExecution {
  val LOGGER = Logger.getLogger(classOf[MoleExecution].getName)
}

class MoleExecution(val mole: IMole, environmentSelection: IEnvironmentSelection, moleJobGrouping: IMoleJobGrouping) extends IMoleExecution {

  def this(mole: IMole) = this(mole, FixedEnvironmentSelection.Empty, MoleJobGrouping.Empty)
    
  def this(mole: IMole, environmentSelection: IEnvironmentSelection) = this(mole, environmentSelection, MoleJobGrouping.Empty)
      
  import MoleExecution._
  
  class MoleExecutionAdapterForMoleJobOutputTransitionPerformed extends IObjectListener[IMoleJob] {
    override def eventOccured(job: IMoleJob) = jobOutputTransitionsPerformed(job)
  }

  class MoleExecutionAdapterForMoleJob extends IObjectListener[IMoleJob] {
    override def eventOccured(job: IMoleJob) = {
      
      //Logger.getLogger(classOf[MoleExecution].getName).fine("Event " + job.task.name + " " + job.state)

      eventDispatcher.objectChanged(MoleExecution.this, IMoleExecution.OneJobStatusChanged, Array(job))
      
      job.state match {
        case State.FAILED => jobFailed(job)
        case _ =>
      }
    }
  }

  class MoleExecutionAdapterForSubMoleExecution extends IObjectListener[ISubMoleExecution] {
    override def eventOccured(obj: ISubMoleExecution) = submitGroups(obj)
  }
    
   
  private val jobs = new LinkedBlockingQueue[(IJob, IEnvironment)] 
  private var inProgress = new TreeMap[IMoleJob, (ISubMoleExecution, ITicket)] //with SynchronizedMap[IMoleJob, (ISubMoleExecution, ITicket)] 

  private val executionId = UUID.randomUUID.toString  
  private val ticketNumber = new AtomicLong

  val rootTicket = Ticket(executionId, ticketNumber.getAndIncrement)
   
  private val currentJobId = new AtomicLong

  val localCommunication = new LocalCommunication
    
  private val categorizer = new DualHashBidiMap[(ISubMoleExecution, IGenericCapsule, IMoleJobGroup), Job]
  private val jobsGrouping = new MultiHashMap[ISubMoleExecution, Job]

  private val moleExecutionAdapterForMoleJob = new MoleExecutionAdapterForMoleJob
  private val moleExecutionAdapterForSubMoleExecution = new MoleExecutionAdapterForSubMoleExecution
  private val moleJobOutputTransitionPerformed = new MoleExecutionAdapterForMoleJobOutputTransitionPerformed
  
  @transient lazy val submiter = {
    val t = new Thread(new Submiter)
    t.setDaemon(true)
    t
  }


  override def register(subMoleExecution: ISubMoleExecution) = {
    eventDispatcher.registerForObjectChangedSynchronous(subMoleExecution, Priority.NORMAL, moleExecutionAdapterForSubMoleExecution, ISubMoleExecution.AllJobsWaitingInGroup)
  }
    
  override def submit(capsule: IGenericCapsule, context: IContext, ticket: ITicket, subMole: ISubMoleExecution): Unit = synchronized {
    val job = capsule.toJob(context, nextJobId)
    submit(job, capsule, subMole, ticket)
  }
   
  def submit(moleJob: IMoleJob, capsule: IGenericCapsule, subMole: ISubMoleExecution, ticket: ITicket): Unit = synchronized {
    eventDispatcher.objectChanged(this, IMoleExecution.OneJobSubmitted, Array(moleJob))

    MoleJobRegistry += moleJob -> (this, capsule)
    eventDispatcher.registerForObjectChangedSynchronous(moleJob, Priority.HIGH, moleExecutionAdapterForMoleJob, IMoleJob.StateChanged)
    eventDispatcher.registerForObjectChangedSynchronous(moleJob, Priority.NORMAL, moleJobOutputTransitionPerformed, IMoleJob.TransitionPerformed)

    inProgress += ((moleJob, (subMole, ticket)))
    subMole.incNbJobInProgress(1)

    moleJobGrouping(capsule) match {
      case Some(strategy) =>
        val category = strategy.group(moleJob.context)

        val key = (subMole, capsule, category)
        val job = categorizer.get(key) match {
          case null =>
            val j = new Job
            categorizer.put(key, j)
            jobsGrouping.put(subMole, j)
            j
          case j => j
        }

        job += moleJob
        subMole.incNbJobWaitingInGroup(1)
      case None =>
        val job = new Job
        job += moleJob
        submit(job, capsule)
    }
  }

  private def submit(job: Job, capsule: IGenericCapsule): Unit = {
    JobRegistry += (job, this)
    environmentSelection.select(capsule) match {
      case Some(environment) =>
        jobs.add((job, environment))
      case None =>
        jobs.add((job, LocalExecutionEnvironment))
    }
  }

  def submitGroups(subMoleExecution: ISubMoleExecution) = synchronized {
    val jobs = jobsGrouping.remove(subMoleExecution)

    for (job <- jobs) {
      val info = categorizer.removeValue(job)
      subMoleExecution.decNbJobWaitingInGroup(job.moleJobs.size)
      submit(job, info._2)
    }
  }


  class Submiter extends Runnable {

    override def run {
      var continue = true
      while (continue) {
        try {
          val p = jobs.take
          try {
            p._2.submit(p._1)
          } catch {
            case (t: Throwable) => LOGGER.log(Level.SEVERE, "Error durring scheduling", t)
          }
        } catch {
          case (e: InterruptedException) =>
            //LOGGER.log(Level.FINE, "Scheduling interrupted", e)
            continue = false
        }
      }
    }
  }
 
  def start(context: IContext) = {
    if (submiter.getState.equals(Thread.State.NEW)) {
      val ticket = nextTicket(rootTicket)

      submit(mole.root, context, ticket, SubMoleExecution(this))
      submiter.start
    } else {
      LOGGER.warning("This MOLE execution has allready been started, this call has no effect.")
    }
  }
  
  @BeforeObjectModified(name = IMoleExecution.Starting)
  override def start = start(new Context)      

  override def cancel = synchronized { 
    submiter.interrupt

    inProgress.synchronized {
      for (moleJob <- inProgress.keySet) {
        moleJob.cancel
      }
      inProgress = TreeMap.empty
    }
  }

  override def moleJobs: Iterable[IMoleJob] = { inProgress.map{ _._1 }}

  override def waitUntilEnded = submiter.join
    
  private def jobFailed(job: IMoleJob) =  jobOutputTransitionsPerformed(job)

  private def jobOutputTransitionsPerformed(job: IMoleJob) = synchronized {
//    eventDispatcher.objectChanged(this, IMoleExecution.OneJobFinished, Array(job))

    val jobInfo = inProgress.get(job) match {
      case None => throw new InternalProcessingError("Error in mole execution job info not found")
      case Some(ji) => ji
    }
    
    inProgress -= job
    
    val subMole = jobInfo._1
    val ticket = jobInfo._2
        
    subMole.decNbJobInProgress(1)

    if (subMole.nbJobInProgess == 0) {
      eventDispatcher.objectChanged(subMole, ISubMoleExecution.Finished, Array(job, this, ticket))
    }

    if (isFinished) {
      submiter.interrupt
      eventDispatcher.objectChanged(this, ISubMoleExecution.Finished, Array(job))
    }

  }

  override def isFinished: Boolean = inProgress.isEmpty

  override def nextTicket(parent: ITicket): ITicket = Ticket(parent, ticketNumber.getAndIncrement)

  override def nextJobId: MoleJobId = new MoleJobId(executionId, currentJobId.getAndIncrement)
    
  override def subMoleExecution(job: IMoleJob): Option[ISubMoleExecution] = {
    inProgress.get(job) match{
      case None => None
      case Some(j) => Some(j._1)
    }
  }
    
  override def ticket(job: IMoleJob): Option[ITicket] = {
    inProgress.get(job) match {
      case None => None
      case Some(j) => Some(j._2)
    }
  }
    
}
