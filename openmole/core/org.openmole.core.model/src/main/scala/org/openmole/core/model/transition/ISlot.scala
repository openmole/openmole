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

package org.openmole.core.model.transition

import org.openmole.core.model.capsule.IGenericCapsule

trait ISlot {

  /**
   *
   * Get all the transitions plugged into this slot.
   *
   * @return all the transitions plugged into this slot
   */
  def transitions: Iterable[IGenericTransition]

  /**
   *
   * Plug <code>transition</code> into this slot.
   *
   * @param transition the transition to plug
   */
  def plugTransition(transition: IGenericTransition)

  /**
   *
   * Unplug <code>transition</code> from thi slot.
   *
   * @param transition the transition to unplug
   */
  def unplugTransition(transition: IGenericTransition)

  /**
   *
   * Test if <code>transition</code> is plugged to this slot.
   *
   * @param transition the transition to test
   * @return true if the transition is plugged to this slot
   */
  def isPlugged(transition: IGenericTransition): Boolean

  /**
   *
   * Get the capsule this slot belongs to.
   *
   * 
   * @return the capsule this slot belongs to
   */
  def capsule: IGenericCapsule
}
