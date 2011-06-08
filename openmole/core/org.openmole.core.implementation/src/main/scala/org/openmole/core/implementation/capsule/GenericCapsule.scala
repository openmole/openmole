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

package org.openmole.core.implementation.capsule

import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.eventdispatcher.IObjectListener
import org.openmole.misc.exception.{InternalProcessingError,UserBadDataError}
import org.openmole.misc.tools.service.Priority
import org.openmole.core.implementation.job.MoleJob
import org.openmole.core.implementation.transition.Slot
import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.core.model.mole.{IMoleExecution,ISubMoleExecution}
import org.openmole.core.model.task.IGenericTask
import org.openmole.core.model.transition.IGenericTransition
import org.openmole.core.model.mole.ITicket
import org.openmole.core.model.data.{IContext,IDataChannel}
import org.openmole.core.model.job.{IMoleJob,MoleJobId}
import org.openmole.core.model.job.State
import org.openmole.core.model.job.State._
import org.openmole.core.model.transition.ISlot
import scala.collection.mutable.HashSet

import org.openmole.core.implementation.mole.MoleJobRegistry
import org.openmole.core.implementation.tools.ToCloneFinder._
import org.openmole.core.implementation.data.DataSet
import org.openmole.core.model.data.IDataSet

abstract class GenericCapsule[TOUT <: IGenericTransition, TASK <: IGenericTask](private var _task: Option[TASK]) extends IGenericCapsule {

  class GenericCapsuleAdapter extends IObjectListener[IMoleJob] {

    override def eventOccured(obj: IMoleJob) = {
      obj.state match {
        case COMPLETED => jobFinished(obj)
        case FAILED | CANCELED => jobFailedOrCanceled(obj)
        case _ =>
      }
    }
  }
  private val _inputSlots = new HashSet[ISlot]
  private val _defaultInputSlot = new Slot(this)
  private val _outputTransitions = new HashSet[TOUT]
    
  private val _inputDataChannels = new HashSet[IDataChannel]
  private val _outputDataChannels = new HashSet[IDataChannel]
 
  override def defaultInputSlot: ISlot = _defaultInputSlot

  override def addInputSlot(slot: ISlot): this.type = {
    _inputSlots += slot
    this
  }

  override def addInputDataChannel(dataChannel: IDataChannel): this.type = {
    _inputDataChannels.add(dataChannel)
    this
  }
    
  override def userInputs: IDataSet  = {
    task match {
      case None => DataSet.empty
      case Some(t) => t.userInputs
    }
  }
  
  override def userOutputs: IDataSet  = {
    task match {
      case None => DataSet.empty
      case Some(t) => t.userOutputs
    }
  }
  
  def inputDataChannels: Iterable[IDataChannel] = _inputDataChannels
  def outputDataChannels: Iterable[IDataChannel] = _outputDataChannels
    
  override def outputTransitions: Iterable[TOUT] = _outputTransitions
      
  override def addOutputDataChannel(dataChannel: IDataChannel): this.type = {
    _outputDataChannels += dataChannel
    this
  }

  override def removeInputDataChannel(dataChannel: IDataChannel): this.type = {
    _inputDataChannels -= dataChannel
    this
  }

  override def removeOutputDataChannel(dataChannel: IDataChannel): this.type = {
    _outputDataChannels -= dataChannel
    this
  }

  def addOutputTransition(transition: TOUT): this.type = {
    _outputTransitions += transition
    this
  }

  override def toJob(context: IContext, jobId: MoleJobId): IMoleJob = {
    val task = taskOrException
    
    for (parameter <- task.parameters) {
      if (parameter.`override` || !context.containsVariableWithName(parameter.variable.prototype)) {
        context += parameter.variable
      }
    }

    val job = new MoleJob(task, context, jobId)
    
    EventDispatcher.registerForObjectChangedSynchronous(job, Priority.LOWEST, new GenericCapsuleAdapter, IMoleJob.StateChanged)
    job
  }

  override def intputSlots: Iterable[ISlot] = _inputSlots

  override def task: Option[TASK] = _task
  
  def task_=(task: TASK) = {
    _task = Some(task)
  }

  def task_=(task: Option[TASK]) = {
    _task = task
  }
  
  private def jobFailedOrCanceled(job: IMoleJob) = {
    val execution = MoleJobRegistry.remove(job).getOrElse(throw new InternalProcessingError("BUG: job not registred"))._1
    EventDispatcher.objectChanged(job, IMoleJob.JobFailedOrCanceled, Array(this))
  }
  
  private def jobFinished(job: IMoleJob) = {
    try {
      val execution = MoleJobRegistry.remove(job).getOrElse(throw new InternalProcessingError("BUG: job not registred"))._1
      val subMole = execution.subMoleExecution(job).getOrElse(throw new InternalProcessingError("BUG: submole not registred for job"))   
      val ticket = execution.ticket(job).getOrElse(throw new InternalProcessingError("BUG: ticket not registred for job"))
      
      EventDispatcher.objectChanged(execution, IMoleExecution.JobInCapsuleFinished, Array(job, this))
      performTransition(job.context, ticket, subMole)
    } catch {
      case e => throw new InternalProcessingError(e, "Error at the end of a MoleJob for task " + task)
    } finally {
      EventDispatcher.objectChanged(job, IMoleJob.TransitionPerformed, Array(this))
    }
  }

  protected def performTransition(context: IContext, ticket: ITicket, subMole: ISubMoleExecution) = {    
    if(outputTransitions.size == 1 && outputDataChannels.isEmpty)
      outputTransitions.head.perform(context, ticket, Set.empty, subMole)
    else {
      import subMole.moleExecution
      val toClone = variablesToClone(this, context, moleExecution)

      outputDataChannels.foreach{_.provides(context, ticket, toClone, moleExecution)}
      outputTransitions.foreach{_.perform(context, ticket, toClone, subMole)}
    }
  }

  override def toString: String = task.toString
  
}
