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
import org.openmole.misc.exception.MultipleException
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.eventdispatcher.IObjectListener
import org.openmole.misc.eventdispatcher.IObjectListenerWithArgs
import org.openmole.core.model.job.ITicket
import org.openmole.core.model.job.State
import org.openmole.core.model.mole.IMoleJobGroup
import org.openmole.core.model.mole.IMoleJobGrouping
import org.openmole.core.model.mole.ISubMoleExecution
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.tools.service.Priority
import org.openmole.core.implementation.execution.local.LocalExecutionEnvironment
import org.openmole.core.implementation.task.GenericTask
import org.openmole.core.model.mole.IInstantRerun
import scala.collection.immutable.TreeMap
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._

object MoleExecution {
  val LOGGER = Logger.getLogger(classOf[MoleExecution].getName)
}

class MoleExecution(val mole: IMole, environmentSelection: IEnvironmentSelection, moleJobGrouping: IMoleJobGrouping, instantRerun: IInstantRerun) extends IMoleExecution {

  def this(mole: IMole) = this(mole, FixedEnvironmentSelection.Empty, MoleJobGrouping.Empty, IInstantRerun.empty)
    
  def this(mole: IMole, environmentSelection: IEnvironmentSelection) = this(mole, environmentSelection, MoleJobGrouping.Empty, IInstantRerun.empty)
      
  def this(mole: IMole, environmentSelection: IEnvironmentSelection, moleJobGrouping: IMoleJobGrouping) = this(mole, environmentSelection, moleJobGrouping, IInstantRerun.empty)
  
  def this(mole: IMole, instantRerun: IInstantRerun) = this(mole, FixedEnvironmentSelection.Empty, MoleJobGrouping.Empty, instantRerun)
    
  def this(mole: IMole, environmentSelection: IEnvironmentSelection, instantRerun: IInstantRerun) = this(mole, environmentSelection, MoleJobGrouping.Empty, instantRerun)

  import IMoleExecution._
  import MoleExecution._
  
  class MoleExecutionAdapterForMoleJobOutputTransitionPerformed extends IObjectListenerWithArgs[IMoleJob] {
    override def eventOccured(job: IMoleJob, args: Array[Object]) = {
      val capsule = args(0).asInstanceOf[IGenericCapsule]
      jobOutputTransitionsPerformed(job, capsule)
    }
  }

  class MoleExecutionAdapterForMoleJobFailed extends IObjectListenerWithArgs[IMoleJob] {
    override def eventOccured(job: IMoleJob, args: Array[Object]) = {
      val capsule = args(0).asInstanceOf[IGenericCapsule]
      jobFailed(job, capsule)
    }
  }
  
  class MoleExecutionAdapterForMoleJob extends IObjectListener[IMoleJob] {
    override def eventOccured(job: IMoleJob) = {
      EventDispatcher.objectChanged(MoleExecution.this, IMoleExecution.OneJobStatusChanged, Array(job))
    }
  }

  class MoleExecutionAdapterForSubMoleExecution extends IObjectListener[ISubMoleExecution] {
    override def eventOccured(obj: ISubMoleExecution) = submitGroups(obj)
  }
 
  private val jobs = new LinkedBlockingQueue[(IJob, IEnvironment)] 
  private var inProgress = new TreeMap[IMoleJob, (ISubMoleExecution, ITicket)] //with SynchronizedMap[IMoleJob, (ISubMoleExecution, ITicket)] 

  private val executionId = UUID.randomUUID.toString  
  private val ticketNumber = new AtomicLong
  private val currentJobId = new AtomicLong

  private val categorizer = new DualHashBidiMap[(ISubMoleExecution, IGenericCapsule, IMoleJobGroup), Job]
  private val jobsGrouping = new MultiHashMap[ISubMoleExecution, Job]

  private val moleExecutionAdapterForMoleJob = new MoleExecutionAdapterForMoleJob
  private val moleExecutionAdapterForSubMoleExecution = new MoleExecutionAdapterForSubMoleExecution
  private val moleJobOutputTransitionPerformed = new MoleExecutionAdapterForMoleJobOutputTransitionPerformed
  private val moleExecutionAdapterForMoleJobFailed = new MoleExecutionAdapterForMoleJobFailed

  val rootTicket = Ticket(executionId, ticketNumber.getAndIncrement)
  val localCommunication = new LocalCommunication
  val exceptions = new ListBuffer[Throwable]
  
  @transient lazy val submiter = {
    val t = new Thread(new Submiter)
    t.setDaemon(true)
    t
  }

  override def register(subMoleExecution: ISubMoleExecution) = {
    EventDispatcher.registerForObjectChangedSynchronous(subMoleExecution, Priority.NORMAL, moleExecutionAdapterForSubMoleExecution, ISubMoleExecution.AllJobsWaitingInGroup)
  }
    
  override def submit(capsule: IGenericCapsule, context: IContext, ticket: ITicket, subMole: ISubMoleExecution): Unit = synchronized {
    val job = capsule.toJob(context, nextJobId)
    submit(job, capsule, subMole, ticket)
  }
   
  private def submit(moleJob: IMoleJob, capsule: IGenericCapsule, subMole: ISubMoleExecution, ticket: ITicket): Unit = synchronized {
    EventDispatcher.objectChanged(this, IMoleExecution.OneJobSubmitted, Array(moleJob))

    MoleJobRegistry += moleJob -> (this, capsule)
    EventDispatcher.registerForObjectChangedSynchronous(moleJob, Priority.HIGH, moleExecutionAdapterForMoleJob, IMoleJob.StateChanged)
    EventDispatcher.registerForObjectChangedSynchronous(moleJob, Priority.NORMAL, moleJobOutputTransitionPerformed, IMoleJob.TransitionPerformed)
    EventDispatcher.registerForObjectChangedSynchronous(moleJob, Priority.NORMAL, moleExecutionAdapterForMoleJobFailed, IMoleJob.JobFailed)

    inProgress += moleJob -> (subMole, ticket)
    subMole.incNbJobInProgress(1)

    if(!instantRerun.rerun(moleJob, capsule))  {
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
  }

  private def submit(job: Job, capsule: IGenericCapsule): Unit = {
    JobRegistry += job -> this
    
    environmentSelection.select(capsule) match {
      case Some(environment) => jobs.add((job, environment))
      case None => jobs.add((job, LocalExecutionEnvironment))
    }
  }

  private def submitGroups(subMoleExecution: ISubMoleExecution) = synchronized {
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
 
  def start(context: IContext): this.type = {
    if (submiter.getState.equals(Thread.State.NEW)) {
      val ticket = nextTicket(rootTicket)

      submit(mole.root, context, ticket, SubMoleExecution(this))
      submiter.start
    } else {
      LOGGER.warning("This MOLE execution has allready been started, this call has no effect.")
    }
    this
  }
  
  override def start = {
    EventDispatcher.objectChanged(this, Starting)
    start(new Context)      
  }
  
  override def cancel: this.type = {
    synchronized { 
      submiter.interrupt

      inProgress.synchronized {
        for (moleJob <- inProgress.keySet) {
          moleJob.cancel
        }
        inProgress = TreeMap.empty
      }
    }
    this
  }

  override def moleJobs: Iterable[IMoleJob] = { inProgress.map{ _._1 }}

  override def waitUntilEnded = {
    submiter.join
    if(!exceptions.isEmpty) throw new MultipleException(exceptions)
    this
  }
    
  private def jobFailed(moleJob: IMoleJob, capsule: IGenericCapsule) = {
    exceptions += moleJob.context.value(GenericTask.Exception.prototype).getOrElse(new InternalProcessingError("BUG: Job has failed but no exception can be found"))
    jobOutputTransitionsPerformed(moleJob, capsule)
  }

  private def jobOutputTransitionsPerformed(job: IMoleJob, capsule: IGenericCapsule) = synchronized {
    val jobInfo = inProgress.get(job) match {
      case None => throw new InternalProcessingError("Error in mole execution job info not found")
      case Some(ji) => ji
    }
    
    inProgress -= job
    
    val subMole = jobInfo._1
    val ticket = jobInfo._2
        
    subMole.decNbJobInProgress(1)

    instantRerun.jobFinished(job, capsule)
    
    if (subMole.nbJobInProgess == 0) {
      EventDispatcher.objectChanged(subMole, ISubMoleExecution.Finished, Array(job, this, ticket))
    }

    if (isFinished) {
      submiter.interrupt
      EventDispatcher.objectChanged(this, ISubMoleExecution.Finished, Array(job))
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
