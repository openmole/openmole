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

package org.openmole.core.implementation.capsule

import java.util.logging.Logger
import org.openmole.commons.aspect.eventdispatcher.IObjectListener
import org.openmole.commons.exception.{InternalProcessingError,UserBadDataError}
import org.openmole.commons.tools.service.Priority
import org.openmole.core.implementation.data.Context
import org.openmole.core.implementation.execution.JobRegistry
import org.openmole.core.implementation.internal.Activator
import org.openmole.core.implementation.job.MoleJob
import org.openmole.core.implementation.transition.Slot
import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.core.model.mole.{IMoleExecution,ISubMoleExecution}
import org.openmole.core.model.task.IGenericTask
import org.openmole.core.model.transition.IGenericTransition
import org.openmole.core.model.data.{IContext,IDataChannel}
import org.openmole.core.model.job.{IMoleJob,IMoleJobId,ITicket}
import org.openmole.core.model.job.State._
import org.openmole.core.model.transition.ISlot
import scala.collection.mutable.HashSet

import org.openmole.core.implementation.mole.MoleJobRegistry
import org.openmole.core.implementation.tools.ToCloneFinder.variablesToClone


abstract class GenericCapsule[TOUT <: IGenericTransition, TASK <: IGenericTask](private var _task: Option[TASK]) extends IGenericCapsule {

  class GenericCapsuleAdapter extends IObjectListener[MoleJob] {

    override def eventOccured(obj: MoleJob) = {
      obj.state match {
        case COMPLETED => jobFinished(obj)
        case _ =>
      }
    }
  }
  private val _inputSlots = new HashSet[ISlot]
  private val _defaultInputSlot = new Slot(this)
  private val _outputTransitions = new HashSet[TOUT]
    
  private val _inputDataChannels = new HashSet[IDataChannel]
  private val _outputDataChannels = new HashSet[IDataChannel]
 
  addInputSlot(_defaultInputSlot)

  override def defaultInputSlot: ISlot = _defaultInputSlot

  override def addInputSlot(slot: ISlot): this.type = {
    _inputSlots += slot
    this
  }

  override def plugInputDataChannel(dataChannel: IDataChannel): this.type = {
    _inputDataChannels.add(dataChannel)
    this
  }
    
  def inputDataChannels: Iterable[IDataChannel] = _inputDataChannels
  def outputDataChannels: Iterable[IDataChannel] = _outputDataChannels
    
  override def outputTransitions: Iterable[TOUT] = _outputTransitions
      
  override def plugOutputDataChannel(dataChannel: IDataChannel): this.type = {
    _outputDataChannels += dataChannel
    this
  }

  override def unplugInputDataChannel(dataChannel: IDataChannel): this.type = {
    _inputDataChannels -= dataChannel
    this
  }

  override def unplugOutputDataChannel(dataChannel: IDataChannel): this.type = {
    _outputDataChannels -= dataChannel
    this
  }

  def plugOutputTransition(transition: TOUT): this.type = {
    _outputTransitions += transition
    this
  }

  override def toJob(global: IContext, context: IContext, jobId: IMoleJobId): IMoleJob = {
         
    _task match {
      case Some(t) =>
        val ret = new MoleJob(t, global, context,jobId)
        Activator.getEventDispatcher.registerForObjectChangedSynchronous(ret, Priority.LOW, new GenericCapsuleAdapter, IMoleJob.StateChanged)
        Activator.getEventDispatcher.objectChanged(this, IGenericCapsule.JobCreated, Array[Object](ret))
        return ret
      case None => throw new UserBadDataError("Reached a capsule with unassigned task.")
    }

  }

  override def intputSlots: Iterable[ISlot] = _inputSlots

  override def task: Option[TASK] = {
    _task
  }
  
  def task_=(task: TASK) = {
    _task = Some(task)
  }

  def task_=(task: Option[TASK]) = {
    _task = task
  }
  
  private def jobFinished(job: MoleJob) = {
    try {
      val execution = MoleJobRegistry.remove(job) match {
        case None => throw new InternalProcessingError("BUG: job not registred")
        case Some(e) => e
      }
      val subMole = execution.subMoleExecution(job) match {
        case None => throw new InternalProcessingError("BUG: submole not registred for job")
        case Some(sm) => sm
      }
            
      val ticket = execution.ticket(job) match {
        case None => throw new InternalProcessingError("BUG: ticket not registred for job")
        case Some(t) => t
      }
 
      performTransition(job.globalContext, job.context, ticket, execution, subMole)
    } catch {
      case e => throw new InternalProcessingError(e, "Error at the end of a MoleJob for task " + task)
    } finally {
      Activator.getEventDispatcher.objectChanged(job, IMoleJob.TransitionPerformed);
    }
  }

  protected def performTransition(global: IContext, context: IContext, ticket: ITicket, moleExecution: IMoleExecution, subMole: ISubMoleExecution) = {
    if(outputTransitions.size == 1 && outputDataChannels.isEmpty)
      outputTransitions.head.perform(global, context, ticket, Set.empty, moleExecution, subMole)
    else {
      val toClone = variablesToClone(this, global, context, moleExecution)
 
      for (dataChannel <- outputDataChannels) {
        dataChannel.provides(context, ticket, toClone, moleExecution);
      }

      for (transition <- outputTransitions) {
        transition.perform(global, context, ticket, toClone, moleExecution, subMole)
      }
    }
  }

  override def toString: String = task.toString
  
}
