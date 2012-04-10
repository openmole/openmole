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

import org.openmole.core.implementation.data.Context
import org.openmole.core.model.mole.IMoleJobGroup
import org.openmole.core.model.mole.ISubMoleExecution
import org.openmole.core.model.mole.ITicket
import org.openmole.core.model.transition.IAggregationTransition
import org.openmole.core.model.transition.ITransition
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.core.implementation.execution.local.LocalExecutionEnvironment
import org.openmole.core.implementation.job.Job
import org.openmole.core.implementation.tools.RegistryWithTicket
import org.openmole.core.model.mole.IAtomicCapsule
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.mole.IMasterCapsule
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.job.IJob
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.job.IMoleJob._
import org.openmole.core.model.mole.IGroupingStrategy
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.job.State._
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.exception.InternalProcessingError
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
  
  private var _nbJobs = 0
  
  private var _childs = new HashSet[SubMoleExecution] with SynchronizedSet[SubMoleExecution]
  private var _jobs = TreeMap.empty[IMoleJob, (ICapsule, ITicket)]
  
  private var canceled = false
  
  val masterCapsuleRegistry = new RegistryWithTicket[IMasterCapsule, IContext]
  val aggregationTransitionRegistry = new RegistryWithTicket[IAggregationTransition, Buffer[IVariable[_]]]
  val transitionRegistry = new RegistryWithTicket[ITransition, Buffer[IVariable[_]]]

  parrentApply(_.+=(this))
  
  def nbJobs_+=(v: Int): Unit = {
    _nbJobs += v
    parrentApply(_.nbJobs_+=(v))
  }
  
  override def isRoot = !parent.isDefined
  
  def nbJobInProgress: Int = synchronized { _nbJobs }
  
  override def cancel = synchronized {
    canceled = true
    _childs.foreach{_.cancel} 
    _jobs.keys.foreach{_.cancel}
    parrentApply(_.-=(this))
  }
  
  override def childs = _childs.toList
  
  private def +=(submoleExecution: SubMoleExecution) = {
    _childs += submoleExecution
  }

  private def -=(submoleExecution: SubMoleExecution) = {
    _childs -= submoleExecution
  }
  
  override def jobs = {
    _jobs.keys.toList ::: childs.flatMap(_.jobs.toList)
  }
  
  private def jobFailedOrCanceled(job: IMoleJob) = synchronized {
    val (capsule, ticket) = _jobs.get(job).getOrElse(throw new InternalProcessingError("Bug, job has not been registred."))    
    _jobs -= job
    nbJobs_+=(-1)
    checkFinished(ticket)
    moleExecution.jobFailedOrCanceled(job, capsule)
  }
  
  private def jobFinished(job: IMoleJob) = synchronized {
    val (capsule, ticket) = _jobs.get(job).getOrElse(throw new InternalProcessingError("Bug, job has not been registred."))
    _jobs -= job
    nbJobs_+=(-1)
    try {
      capsule match {
        case c: IMasterCapsule => 
          masterCapsuleRegistry.register(
            c,
            ticket.parentOrException,
            c.toPersist(job.context)
          )
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
    } 
  }
  
  private def checkFinished(ticket: ITicket) = 
    if (nbJobInProgress == 0) {
      EventDispatcher.trigger(this, new ISubMoleExecution.Finished(ticket))
      parrentApply(_.-=(this))
    }
  
  override def submit(capsule: ICapsule, context: IContext, ticket: ITicket) = synchronized {
    if(!canceled) {
      def implicits = Context.empty ++ moleExecution.mole.implicits.values.filter(v => capsule.taskOrException.inputs.contains(v.prototype.name))
      
      val moleJob: IMoleJob = capsule match {
        case c: IMasterCapsule => 
          val savedContext = masterCapsuleRegistry.remove(c, ticket.parentOrException).getOrElse(new Context)
          new MoleJob(capsule.taskOrException, implicits + context + savedContext, moleExecution.nextJobId, stateChanged)
        case _ => new MoleJob(capsule.taskOrException, implicits + context, moleExecution.nextJobId, stateChanged)
      }
      
      _jobs += (moleJob -> (capsule, ticket))
      
      nbJobs_+=(1)
      
      val instant = 
        synchronized {
          EventDispatcher.trigger(moleExecution, new IMoleExecution.JobInCapsuleStarting(moleJob, capsule))
          EventDispatcher.trigger(moleExecution, new IMoleExecution.OneJobSubmitted(moleJob))
          moleExecution.instantRerun(moleJob, capsule)
        }
      
      if(!instant)
        capsule match {
          case _: IAtomicCapsule => moleJob.perform
          case _ => moleExecution.group(moleJob, capsule)
        }
    }
  }

  def newChild: ISubMoleExecution = synchronized { 
    val subMole = new SubMoleExecution(Some(this), moleExecution) 
    if(canceled) subMole.cancel
    subMole
  } 
  
  
  private def parrentApply(f: SubMoleExecution => Unit) = 
    parent match {
      case None => 
      case Some(p) => f(p)
    }
   
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
