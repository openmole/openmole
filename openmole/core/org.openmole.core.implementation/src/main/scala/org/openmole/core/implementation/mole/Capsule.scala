/*
 * Copyright (C) 2010 Romain Reuillon
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

import org.openmole.core.implementation.transition._
import org.openmole.core.model.mole._
import org.openmole.core.model.task._
import org.openmole.core.model.transition._
import org.openmole.core.model.data._
import org.openmole.core.model.job._
import org.openmole.core.model.job.State._

import scala.collection.mutable.ListBuffer

class Capsule(var _task: Option[ITask] = None) extends ICapsule {

  def this(t: ITask) = this(Some(t))

  private val _inputSlots = new ListBuffer[ISlot]
  private val _defaultInputSlot = new Slot(this)
  private val _outputTransitions = new ListBuffer[ITransition]

  private val _outputIDataChannels = new ListBuffer[IDataChannel]

  override def inputs: DataSet =
    task match {
      case None ⇒ DataSet.empty
      case Some(t) ⇒ t.inputs
    }

  override def outputs: DataSet =
    task match {
      case None ⇒ DataSet.empty
      case Some(t) ⇒ t.outputs
    }

  override def defaultInputSlot: ISlot = _defaultInputSlot

  override def addInputSlot(slot: ISlot): this.type = {
    _inputSlots += slot
    this
  }

  def outputIDataChannels: Iterable[IDataChannel] = _outputIDataChannels

  override def outputTransitions: Iterable[ITransition] = _outputTransitions

  override def addOutputIDataChannel(dataChannel: IDataChannel): this.type = {
    _outputIDataChannels += dataChannel
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
    case Some(t) ⇒ t.toString
    case None ⇒ "[None]"
  }

}
