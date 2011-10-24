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

import org.openmole.core.model.mole.IMoleJobGroup
import org.openmole.core.model.mole.IMoleJobGrouping
import org.openmole.core.model.mole.ISubMoleExecution
import org.openmole.core.model.mole.ITicket
import org.openmole.core.model.task.ITask
import org.openmole.core.model.transition.IAggregationTransition
import org.openmole.core.model.transition.ITransition
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.core.implementation.job.Job
import org.openmole.core.implementation.tools.RegistryWithTicket
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.job.IJob
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.job.IMoleJob._
import org.openmole.core.model.mole.IGroupingStrategy
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.eventdispatcher.Event
import org.openmole.misc.eventdispatcher.EventListener
import org.openmole.misc.tools.service.Priority
import scala.collection.immutable.TreeSet
import scala.collection.mutable.Buffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer

object SubMoleExecution {
  
  def apply(moleExecution: IMoleExecution) = new SubMoleExecution(None, moleExecution)

  def apply(moleExecution: IMoleExecution, parent: ISubMoleExecution) = new SubMoleExecution(Some(parent), moleExecution)
  
}


class SubMoleExecution(val parent: Option[ISubMoleExecution], val moleExecution: IMoleExecution) extends ISubMoleExecution {

  val subMoleExecutionAdapterForMoleJob = new EventListener[IMoleJob] {
    override def triggered(job: IMoleJob, ev: Event[IMoleJob]) = 
      ev match {
        case ev: IMoleJob.TransitionPerformed => jobFinished(job, ev.capsule)
        case ev: IMoleJob.JobFailedOrCanceled => jobFinished(job, ev.capsule)
      }
  }
  
  private var submittedJobs = TreeSet[IMoleJob]()
  private var waiting = List[IJob]()
  private var _nbJobInProgress = 0
  private var _nbJobWaitingInGroup = 0
  private var childs = new HashSet[ISubMoleExecution]
  private val waitingJobs = new HashMap[(ICapsule, IMoleJobGroup), ListBuffer[IMoleJob]]
  
  private var canceled = false
  
  val masterTransitionRegistry = new RegistryWithTicket[IAggregationTransition, IContext]
  val aggregationTransitionRegistry = new RegistryWithTicket[IAggregationTransition, Buffer[IVariable[_]]]
  val transitionRegistry = new RegistryWithTicket[ITransition, Buffer[IVariable[_]]]

  parrentApply(_.addChild(this))

  override def isRoot = !parent.isDefined
  
  override def nbJobInProgess = _nbJobInProgress //_nbJobInProgress
  
  def += (moleJob: IMoleJob) = synchronized {
    submittedJobs += moleJob
    incNbJobInProgress(1)
  }
  
  def -= (moleJob: IMoleJob) = synchronized {
    submittedJobs -= moleJob
    decNbJobInProgress(1)
  }
  
  override def cancel = synchronized {
    submittedJobs.foreach{_.cancel}
    childs.foreach{_.cancel}
    parrentApply(_.removeChild(this))
    canceled = true
  }
  
  override def addChild(submoleExecution: ISubMoleExecution) = synchronized {
    childs += submoleExecution
  }

  override def removeChild(submoleExecution: ISubMoleExecution) = synchronized {
    childs -= submoleExecution
  }
  
  override def incNbJobInProgress(nb: Int) =  {
    synchronized {_nbJobInProgress += nb}
    parrentApply(_.incNbJobInProgress(nb))
  }

  override def decNbJobInProgress(nb: Int) = {
    if(synchronized{_nbJobInProgress -= nb; checkAllJobsWaitingInGroup}) submitJobs
    parrentApply(_.decNbJobInProgress(nb))
  }
  
  override def incNbJobWaitingInGroup(nb: Int) = {
    if(synchronized {_nbJobWaitingInGroup += nb; checkAllJobsWaitingInGroup}) submitJobs
    parrentApply(_.incNbJobWaitingInGroup(nb))
  }

  override def decNbJobWaitingInGroup(nb: Int) = synchronized {
    _nbJobWaitingInGroup -= nb
    parrentApply(_.decNbJobWaitingInGroup(nb))
  }
  
  override def submit(capsule: ICapsule, context: IContext, ticket: ITicket) = synchronized {
    if(!canceled) {
      val moleJob = capsule.toJob(context, moleExecution.nextJobId)

      EventDispatcher.listen(moleJob, Priority.HIGH, subMoleExecutionAdapterForMoleJob, classOf[IMoleJob.TransitionPerformed])
      EventDispatcher.listen(moleJob, Priority.HIGH, subMoleExecutionAdapterForMoleJob, classOf[IMoleJob.JobFailedOrCanceled])

      this += moleJob
      
      moleExecution.submit(moleJob, capsule, this, ticket)
    }
  }

  override def group(moleJob: IMoleJob, capsule: ICapsule, grouping: Option[IGroupingStrategy]) = synchronized {
    grouping match {
      case Some(strategy) =>
        val category = strategy.group(moleJob.context)

        val key = (capsule, category)
        waitingJobs.getOrElseUpdate(key, new ListBuffer[IMoleJob]) += moleJob
        incNbJobWaitingInGroup(1)
      case None =>
        val job = new Job(moleExecution.id, List(moleJob))
        moleExecution.submitToEnvironment(job, capsule)
    }
  }
    
  private def submitJobs = synchronized {
    waitingJobs.foreach {
      case((capsule, category), jobs) => 
        val job = new Job(moleExecution.id, jobs)
        moleExecution.submitToEnvironment(job, capsule)
        decNbJobWaitingInGroup(job.moleJobs.size)
    }
    waitingJobs.empty
  }
  
  private def parrentApply(f: ISubMoleExecution => Unit) = 
    parent match {
      case None => 
      case Some(p) => f(p)
    }
    
  private def jobFinished(job: IMoleJob, capsule: ICapsule): Unit = synchronized {       
    this -= job
  }
  
  private def checkAllJobsWaitingInGroup = (nbJobInProgess == _nbJobWaitingInGroup && _nbJobWaitingInGroup > 0)
  
  //private def allWaitingEvent = EventDispatcher.objectChanged(this, ISubMoleExecution.AllJobsWaitingInGroup)
  
}
