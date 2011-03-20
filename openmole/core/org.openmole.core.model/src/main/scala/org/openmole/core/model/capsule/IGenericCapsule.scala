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

package org.openmole.core.model.capsule

import org.openmole.core.model.data.{IDataChannel,IContext, IDataSet}
import org.openmole.core.model.job.{IMoleJob,MoleJobId}
import org.openmole.core.model.task.IGenericTask
import org.openmole.core.model.transition.{IGenericTransition,ISlot}


/**
 * Super class for all capsules. Capsules are entities linked together by
 * transition to build a workflow. Even if it may be not true fo technical
 * reasons, conceptually they are this is the only mutable class element of
 * the workflow representation.
 * 
 */
trait IGenericCapsule {

  /**
   * Get the Some(task) assigned to this capsule or None if not the task has not
   * been assigned.
   *
   * @return Some(task) inside this capsule or None if not the task has not been assigned
   */
  def task: Option[IGenericTask]
  
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
   * Get all the output transitions plugged to this capsule.
   *
   * @return all the output transitions plugged to this capsule
   */
  def outputTransitions: Iterable[IGenericTransition]

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
   *
   * Get the user input data of the task contained in this capsule.
   *
   * @return the input of the task
   */
  def userInputs: IDataSet  
   
  /**
   *
   * Get the user output data of the task contained in this capsule.
   *
   * @return the output data of the task
   */
  def userOutputs: IDataSet
}
