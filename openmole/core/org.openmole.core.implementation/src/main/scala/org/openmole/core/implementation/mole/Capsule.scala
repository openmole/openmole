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

import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.eventdispatcher.Event
import org.openmole.misc.eventdispatcher.EventListener
import org.openmole.misc.exception.{InternalProcessingError,UserBadDataError}
import org.openmole.misc.tools.service.Priority
import org.openmole.core.implementation.job.MoleJob
import org.openmole.core.implementation.transition.Slot
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.mole.{IMoleExecution,ISubMoleExecution}
import org.openmole.core.model.task.ITask
import org.openmole.core.model.transition.ITransition
import org.openmole.core.model.mole.ITicket
import org.openmole.core.model.data.{IContext,IDataChannel}
import org.openmole.core.model.job.{IMoleJob,MoleJobId}
import org.openmole.core.model.job.State
import org.openmole.core.model.job.State._
import org.openmole.core.model.transition.ISlot
import scala.collection.mutable.HashSet

import org.openmole.core.implementation.data.DataSet
import org.openmole.core.model.data.IDataSet

class Capsule(var _task: Option[ITask] = None) extends ICapsule {

  def this(t: ITask) = this(Some(t))

  class CapsuleAdapter extends EventListener[IMoleJob] {

    override def triggered(obj: IMoleJob, ev: Event[IMoleJob]) = 
      ev match {
        case ev: IMoleJob.StateChanged => 
          ev.newState match {
            case COMPLETED => jobFinished(obj)
            case FAILED | CANCELED => jobFailedOrCanceled(obj)
            case _ =>
          }
     }
  }
  
  private val _inputSlots = new HashSet[ISlot]
  private val _defaultInputSlot = new Slot(this)
  private val _outputTransitions = new HashSet[ITransition]
    
  private val _inputDataChannels = new HashSet[IDataChannel]
  private val _outputDataChannels = new HashSet[IDataChannel]

  
  override def inputs: IDataSet  = 
    task match {
      case None => DataSet.empty
      case Some(t) => t.inputs
    }
  
  override def outputs: IDataSet =
    task match {
      case None => DataSet.empty
      case Some(t) => t.outputs
    }
  
  override def defaultInputSlot: ISlot = _defaultInputSlot

  override def addInputSlot(slot: ISlot): this.type = {
    _inputSlots += slot
    this
  }

  override def addInputDataChannel(dataChannel: IDataChannel): this.type = {
    _inputDataChannels.add(dataChannel)
    this
  }
  
  def inputDataChannels: Iterable[IDataChannel] = _inputDataChannels
  def outputDataChannels: Iterable[IDataChannel] = _outputDataChannels
    
  override def outputTransitions: Iterable[ITransition] = _outputTransitions
      
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

  def addOutputTransition(transition: ITransition): this.type = {
    _outputTransitions += transition
    this
  }

  override def toJob(context: IContext, jobId: MoleJobId): IMoleJob = {
    val task = taskOrException
    val job: IMoleJob = new MoleJob(task, context, jobId)
    
    EventDispatcher.listen(job, Priority.LOWEST, new CapsuleAdapter, classOf[IMoleJob.StateChanged])
    job
  }

  override def intputSlots: Iterable[ISlot] = _inputSlots

  override def task_=(task: ITask) = this.task = Some(task) 
  override def task_=(task: Option[ITask]) = _task = task
  override def task = _task
  
  private def jobFailedOrCanceled(job: IMoleJob) = {
    val execution = MoleJobRegistry.remove(job).getOrElse(throw new InternalProcessingError("BUG: job not registred"))._1
    EventDispatcher.trigger(job, new IMoleJob.JobFailedOrCanceled(this))
  }
  
  private def jobFinished(job: IMoleJob) = {
    try {
      val execution = MoleJobRegistry.remove(job).getOrElse(throw new InternalProcessingError("BUG: job not registred"))._1
      val subMole = execution.subMoleExecution(job).getOrElse(throw new InternalProcessingError("BUG: submole not registred for job"))   
      val ticket = execution.ticket(job).getOrElse(throw new InternalProcessingError("BUG: ticket not registred for job"))
      
      EventDispatcher.trigger(execution, new IMoleExecution.JobInCapsuleFinished(job, this))
      performTransition(job.context, ticket, subMole)
    } catch {
      case e => throw new InternalProcessingError(e, "Error at the end of a MoleJob for task " + task)
    } finally EventDispatcher.trigger(job, new IMoleJob.TransitionPerformed(this))
  }

  protected def performTransition(context: IContext, ticket: ITicket, subMole: ISubMoleExecution) = {    
    import subMole.moleExecution

    outputDataChannels.foreach{_.provides(context, ticket, moleExecution)}
    outputTransitions.foreach{_.perform(context, ticket, subMole)}
  }

  override def toString: String = task.toString
  
}
