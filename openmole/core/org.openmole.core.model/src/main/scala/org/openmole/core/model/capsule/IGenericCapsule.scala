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

import org.openmole.core.model.data.{IDataChannel,IContext}
import org.openmole.core.model.job.{IMoleJob,IMoleJobId}
import org.openmole.core.model.task.IGenericTask
import org.openmole.core.model.transition.{IGenericTransition,ISlot}

object IGenericCapsule {
  val JobCreated = "JobCreated"
}

trait IGenericCapsule {

  /**
   *
   * Get the task assigned to this capsule or null if not the task has not been assigned.
   *
   * @return task inside this capsule or null if not the task has not been assigned
   */
  def task: Option[IGenericTask]
  
  /**
   *
   * Get the list of data channels ending at this capsule.
   *
   * @return the list of data channels ending at this capsule
   */
  def inputDataChannels: Iterable[IDataChannel]

  /**
   *
   * Get the list of data channels starting at this capsule.
   *
   * @return the list of data channels starting at this capsule
   */
  def outputDataChannels: Iterable[IDataChannel]

  /**
   *
   * Plug a datachannel in input of this capsule.
   *
   * @param dataChannel the datachannel to plug
   */
  def plugInputDataChannel(dataChannel: IDataChannel): this.type

  /**
   *
   * Plug a datachannel in output of this capsule.
   *
   * @param dataChannel the datachannel to plug
   */
  def plugOutputDataChannel(dataChannel: IDataChannel): this.type

  /**
   *
   * Unplug a datachannel in input of this capsule.
   *
   * @param dataChannel   the datachannel to unplug
   */
  def unplugInputDataChannel(dataChannel: IDataChannel): this.type

  /**
   *
   * Unplug a datachannel in output of this capsule.
   *
   * @param dataChannel   the datachannel to unplug
   */
  def unplugOutputDataChannel(dataChannel: IDataChannel): this.type


  /**
   *
   * Get the default input slot of this capsule.
   *
   * @return the default input slot of this capsule
   */
  def defaultInputSlot: ISlot

  /**
   *
   * Get all the input slots of this capsule.
   *
   * @return the input slots of this capsule
   */
  def intputSlots: Iterable[ISlot]

  /**
   * .
   * 
   * @param group the transition slot to be added.
   */
  def addInputSlot(group: ISlot): this.type

  /**
   *
   * Get all the output transitions plugged to this capsule.
   *
   * @return all the output transitions plugged to this capsule
   */
  def outputTransitions: Iterable[IGenericTransition]

  /**
   *
   * Instanciate a job from this capsule.
   *
   * @param context the context in which the job will be executed
   * @param ticket the ticket in which the job will be executed
   * @param jobId the id of the job
   * @return the job
   */
  def toJob(global: IContext, context: IContext, jobId: IMoleJobId): IMoleJob

}
