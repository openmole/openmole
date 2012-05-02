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

package org.openmole.core.model.transition

import org.openmole.core.model.data.IDataChannel
import org.openmole.core.model.mole.ICapsule

trait ISlot {

  /**
   *
   * Get all the transitions plugged into this slot.
   *
   * @return all the transitions plugged into this slot
   */
  def transitions: Iterable[ITransition]

  /**
   *
   * Plug <code>transition</code> into this slot.
   *
   * @param transition the transition to plug
   */
  def +=(transition: ITransition): this.type
  def add(transition: ITransition): this.type = { +=(transition) }

  /**
   *
   * Unplug <code>transition</code> from thi slot.
   *
   * @param transition the transition to unplug
   */
  //def -=(transition: ITransition): this.type
  //def remove(transition: ITransition): this.type = { -=(transition)}

  /**
   *
   * Test if <code>transition</code> is plugged to this slot.
   *
   * @param transition the transition to test
   * @return true if the transition is plugged to this slot
   */
  def contains(transition: ITransition): Boolean

  /**
   *
   * Get the capsule this slot belongs to.
   *
   *
   * @return the capsule this slot belongs to
   */
  def capsule: ICapsule

  /**
   * Get all data channels ending at this slot.
   *
   * @return all of data channels ending at this slot
   */
  def inputDataChannels: Iterable[IDataChannel]

  /**
   * Add a datachannel to the input data channels of this slot.
   *
   * @param dataChannel the datachannel to plug
   * @return the capsule itself
   */
  def addInputDataChannel(dataChannel: IDataChannel): this.type
}
