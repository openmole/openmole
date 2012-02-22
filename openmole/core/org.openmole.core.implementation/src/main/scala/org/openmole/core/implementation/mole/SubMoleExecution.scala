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

import java.util.logging.Level
import org.openmole.core.implementation.data.Context
import org.openmole.core.model.mole.IMoleJobGroup
import org.openmole.core.model.mole.ISubMoleExecution
import org.openmole.core.model.mole.ITicket
import org.openmole.core.model.transition.IAggregationTransition
import org.openmole.core.model.transition.ITransition
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.core.implementation.job.Job
import org.openmole.core.implementation.tools.RegistryWithTicket
import org.openmole.core.model.mole.IAtomicCapsule
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.mole.IMasterCapsule
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.job.IMoleJob._
import org.openmole.core.model.mole.IGroupingStrategy
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.job.State._
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.eventdispatcher.Event
import org.openmole.misc.eventdispatcher.EventListener
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.tools.service.Priority
import org.openmole.core.implementation.job.MoleJob
import org.openmole.core.implementation.job.MoleJob._
import scala.collection.immutable.TreeMap
import scala.collection.mutable.Buffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer
import collection.JavaConversions._
import scala.collection.mutable.SynchronizedSet



class SubMoleExecution(val parent: Option[SubMoleExecution], val moleExecution: MoleExecution) extends ISubMoleExecution {
  
  /*val jobListener = new EventListener[IMoleJob] {

   override def triggered(job: IMoleJob, ev: Event[IMoleJob]) = 
   ev match {
   case ev: IMoleJob.StateChanged => 
   ev.newState match {
   case COMPLETED => jobFinished(job)
   case FAILED | CANCELED => jobFailedOrCanceled(job)
   case _ =>
   }
   EventDispatcher.trigger(moleExecution, new IMoleExecution.OneJobStatusChanged(job, ev.newState, ev.oldState))
   case ev: IMoleJob.ExceptionRaised => 
   EventDispatcher.trigger(moleExecution, new IMoleExecution.ExceptionRaised(job, ev.exception, ev.level))

   }
   }*/
  
  private var _submitting = false
  private var _nbJobGrouping = 0
  
  private var _childs = new HashSet[SubMoleExecution] with SynchronizedSet[SubMoleExecution]
  private var _jobs = TreeMap.empty[IMoleJob, (ICapsule, ITicket)]
  
  private val waitingJobs = new HashMap[(ICapsule, IMoleJobGroup), ListBuffer[IMoleJob]]
  private var canceled = false
  
  val masterCapsuleRegistry = new RegistryWithTicket[IMasterCapsule, Iterable[IVariable[_]]]
  val aggregationTransitionRegistry = new RegistryWithTicket[IAggregationTransition, Buffer[IVariable[_]]]
  val transitionRegistry = new RegistryWithTicket[ITransition, Buffer[IVariable[_]]]

  parrentApply(_.+=(this))
  
  override def isRoot = !parent.isDefined
  
  def nbJobInProgress: Int = {
    _jobs.size + childs.map{_.nbJobInProgress}.sum
  }

  def nbJobGrouping: Int = {
    _nbJobGrouping + childs.map{_.nbJobGrouping}.sum
  }
  
  override def submitting_=(b: Boolean) = {
    _submitting = b
    if(allJobsWaitingInGroup) submitJobs
  }
  
  override def cancel = synchronized {
    _jobs.keys.foreach{_.cancel}
    _childs.foreach{_.cancel}
    parrentApply(_.-=(this))
    canceled = true
  }
  
  override def childs = _childs.toList
  
  def +=(submoleExecution: SubMoleExecution) = {
    _childs += submoleExecution
  }

  def -=(submoleExecution: SubMoleExecution) = {
    _childs -= submoleExecution
  }
  
  override def jobs = {
    _jobs.keys.toList ::: childs.flatMap(_.jobs.toList)
  }
  
  private def jobFailedOrCanceled(job: IMoleJob) = synchronized {
    val (capsule, ticket) = _jobs.get(job).getOrElse(throw new InternalProcessingError("Bug, job has not been registred."))    
    _jobs -= job
    
    checkFinished(ticket)
    moleExecution.jobFailedOrCanceled(job, capsule)
//    EventDispatcher.trigger(job, new IMoleJob.JobFailedOrCanceled(capsule))
  }
  
  private def jobFinished(job: IMoleJob) = synchronized {
    val (capsule, ticket) = _jobs.get(job).getOrElse(throw new InternalProcessingError("Bug, job has not been registred."))
    _jobs -= job
    
    try {
      capsule match {
        case c: IMasterCapsule => masterCapsuleRegistry.register(c, ticket.parentOrException, c.toPersist(job.context))
        case _ =>
      }
      
      EventDispatcher.trigger(moleExecution, new IMoleExecution.JobInCapsuleFinished(job, capsule))
      
      capsule.outputDataChannels.foreach{_.provides(job.context, ticket, moleExecution)}
      capsule.outputTransitions.foreach{_.perform(job.context, ticket, this)}
    } catch {
      case e => throw new InternalProcessingError(e, "Error at the end of a MoleJob for capsule " + capsule)
    } finally {
      checkFinished(ticket)
      moleExecution.jobOutputTransitionsPerformed(job, capsule)
  //    EventDispatcher.trigger(job, new IMoleJob.TransitionPerformed(capsule))
    }
    if(allJobsWaitingInGroup) submitJobs
  }
  
  def checkFinished(ticket: ITicket) = 
    if (nbJobInProgress == 0) {
      EventDispatcher.trigger(this, new ISubMoleExecution.Finished(ticket))
      parrentApply(_.-=(this))
    }
  
  override def submit(capsule: ICapsule, context: IContext, ticket: ITicket) = synchronized {
    if(!canceled) {
      val moleJob: IMoleJob = capsule match {
        case c: IMasterCapsule => 
          val savedContext = masterCapsuleRegistry.remove(c, ticket.parentOrException).getOrElse(new Context)
          new MoleJob(capsule.taskOrException, context ++ savedContext, moleExecution.nextJobId, stateChanged)
        case _ => new MoleJob(capsule.taskOrException, context, moleExecution.nextJobId, stateChanged)
      }
      
      _jobs += (moleJob -> (capsule, ticket))
      // EventDispatcher.listen(moleJob, Priority.HIGH, jobListener, classOf[IMoleJob.StateChanged])
      // EventDispatcher.listen(moleJob, Priority.NORMAL, jobListener, classOf[IMoleJob.ExceptionRaised])

      moleExecution.submit(moleJob, capsule, this, ticket)
    }
  }

  override def group(moleJob: IMoleJob, capsule: ICapsule, grouping: Option[IGroupingStrategy]) = synchronized {
    capsule match {
      case _: IAtomicCapsule => moleJob.perform
      case _ => 
        grouping match {
          case Some(strategy) =>
            val category = strategy.group(moleJob.context)
            val key = (capsule, category)
            waitingJobs.getOrElseUpdate(key, new ListBuffer[IMoleJob]) += moleJob
            _nbJobGrouping += 1
          case None =>
            val job = new Job(moleExecution.id, List(moleJob))
            moleExecution.submitToEnvironment(job, capsule)
        }
    }
  }
    
  def newChild: ISubMoleExecution = new SubMoleExecution(Some(this), moleExecution)
  
  private def submitJobs = synchronized {
    waitingJobs.foreach {
      case((capsule, category), jobs) => 
        val job = new Job(moleExecution.id, jobs)
        moleExecution.submitToEnvironment(job, capsule)
        _nbJobGrouping += job.moleJobs.size
    }
    waitingJobs.empty
  }
  
  private def parrentApply(f: SubMoleExecution => Unit) = 
    parent match {
      case None => 
      case Some(p) => f(p)
    }
   
  private def allJobsWaitingInGroup = (nbJobInProgress == nbJobGrouping && nbJobGrouping > 0 && !_submitting)
   
  def stateChanged(job: IMoleJob, oldState: State, newState: State) = {
    newState match {
      case COMPLETED => jobFinished(job)
      case FAILED | CANCELED => jobFailedOrCanceled(job)
      case _ =>
    }
    EventDispatcher.trigger(moleExecution, new IMoleExecution.OneJobStatusChanged(job, newState, oldState))
    job.exception match {
      case Some(e) => 
        logger.log(SEVERE, "Error in user job execution, job state is FAILED.", e)
        EventDispatcher.trigger(moleExecution, new IMoleExecution.ExceptionRaised(job, e, SEVERE))
      case _ =>
    }
  }
  
}
