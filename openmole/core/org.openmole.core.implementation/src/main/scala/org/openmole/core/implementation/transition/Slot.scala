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

package org.openmole.core.implementation.transition

import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.transition.ITransition
import org.openmole.core.model.transition.ISlot
import scala.collection.mutable.HashSet

class Slot(val capsule: ICapsule) extends ISlot {
  capsule.addInputSlot(this)
  
  private val _transitions = new HashSet[ITransition]

  override def +=(transition: ITransition) = {
    _transitions += transition
    this
  }

  override def -=(transition: ITransition) = {
    _transitions -= transition
    this
  }

  override def transitions: Iterable[ITransition] =  _transitions
    
  override def contains(transition: ITransition) = _transitions.contains(transition)

}
