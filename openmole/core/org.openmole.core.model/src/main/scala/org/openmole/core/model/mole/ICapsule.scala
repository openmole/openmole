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

package org.openmole.core.model.mole

import org.openmole.core.model.data.{IDataSet, IContext, IDataChannel}
import org.openmole.core.model.job.{MoleJobId, IMoleJob}
import org.openmole.core.model.task.ITask
import org.openmole.core.model.transition.{ISlot, ITransition}
import org.openmole.misc.exception.UserBadDataError

/**
 * A capsule containing a task.
 * 
 */
trait ICapsule {

  /**
   * Get the Some(task) assigned to this capsule or None if not the task has not
   * been assigned.
   *
   * @return Some(task) inside this capsule or None if not the task has not been assigned
   */
  def task: Option[ITask]
  
  def taskOrException = task.getOrElse(throw new UserBadDataError("Capsule task is unassigned.")) 
  
  /*
   * Get the inputs data taken by this capsule, generally it is empty if the capsule
   * is empty or the input of the task inside the capsule. It can be different
   * in some cases.
   * 
   * @return the input of the capsule
   */
  def inputs: IDataSet
  
  /*
   * Get the outputs data taken by this capsule, generally it is empty if the capsule
   * is empty or the output of the task inside the capsule. It can be different
   * in some cases.
   * 
   * @return the output of the capsule
   */
  def outputs: IDataSet
  
  /**
   * Get all data channels ending at this capsule.
   *
   * @return all of data channels ending at this capsule
   */
  def inputDataChannels: Iterable[IDataChannel]

  /**
   * Get all data channels starting from this capsule.
   *
   * @return all data channels starting from this capsule
   */
  def outputDataChannels: Iterable[IDataChannel]

  /**
   * Add a datachannel to the input data channels of this capsule.
   *
   * @param dataChannel the datachannel to plug
   * @return the capsule itself
   */
  def addInputDataChannel(dataChannel: IDataChannel): this.type

  /**
   * Add a datachannel in output of this capsule.
   *
   * @param dataChannel the datachannel to add
   * @return the capsule itself
   */
  def addOutputDataChannel(dataChannel: IDataChannel): this.type

  /**
   * Remove a datachannel from the input data chanel of this capsule.
   *
   * @param dataChannel the datachannel to remove
   * @return the capsule itself
   */
  def removeInputDataChannel(dataChannel: IDataChannel): this.type

  /**
   * Remove a datachannel in output of this capsule.
   *
   * @param dataChannel the datachannel to remove
   * @return the capsule itself
   */
  def removeOutputDataChannel(dataChannel: IDataChannel): this.type


  /**
   * Get the default input slot of this capsule.
   *
   * @return the default input slot of this capsule
   */
  def defaultInputSlot: ISlot

  /**
   * Get all the input slots of this capsule.
   *
   * @return all the input slots of this capsule
   */
  def intputSlots: Iterable[ISlot]

  /**
   * Add an input slot to this capsule
   * 
   * @param group the input slot to add.
   */
  def addInputSlot(group: ISlot): this.type


  /**
   * Instanciate a MoleJob from this capsule for running the task contained
   * in it.
   *
   * @param context the context in which the MoleJob will be executed
   * @param jobId the id of the MoleJob
   * @return the MoleJob
   */
  def toJob(context: IContext, jobId: MoleJobId): IMoleJob

  /**
   * Assing a task to this capsule.
   * 
   * @param task the task to assign to this capsule.
   */
  def task_=(task: ITask)
  
  /**
   * Assing an option of task to this capsule.
   * 
   * @param task the option of task to assign to this capsule.
   */
  def task_=(task: Option[ITask])
    
  /**
   * Add an output transition to this capsule.
   * 
   * @param transition the transition to add
   * @return the capsule itself
   */
  def addOutputTransition(transition: ITransition): this.type
  
  /**
   * Get all the output transitions plugged to this capsule.
   *
   * @return all the output transitions plugged to this capsule
   */
  def outputTransitions: Iterable[ITransition]
  
}
