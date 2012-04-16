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

import org.openmole.core.implementation.data.DataSet
import org.openmole.core.model.data.IDataSet
import scala.collection.mutable.ListBuffer

class Capsule(var _task: Option[ITask] = None) extends ICapsule {

  def this(t: ITask) = this(Some(t))
  
  private val _inputSlots = new ListBuffer[ISlot]
  private val _defaultInputSlot = new Slot(this)
  private val _outputTransitions = new ListBuffer[ITransition]
    
  private val _outputDataChannels = new ListBuffer[IDataChannel]

  
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

  def outputDataChannels: Iterable[IDataChannel] = _outputDataChannels
    
  override def outputTransitions: Iterable[ITransition] = _outputTransitions
      
  override def addOutputDataChannel(dataChannel: IDataChannel): this.type = {
    _outputDataChannels += dataChannel
    this
  }

  def addOutputTransition(transition: ITransition): this.type = {
    _outputTransitions += transition
    this
  }

  override def intputSlots: Iterable[ISlot] = _inputSlots

  override def task_=(task: ITask) = this.task = Some(task) 
  override def task_=(task: Option[ITask]) = _task = task
  override def task = _task
  

  override def toString = task match {
    case Some(t) => t.toString
    case None => "[None]"
  }
  
}
