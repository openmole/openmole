/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.model.commons
import org.openmole.misc.exception.UserBadDataError

object TransitionType extends Enumeration {
  type TransitionType = Value
  val BASIC_TRANSITION, EXPLORATION_TRANSITION, AGGREGATION_TRANSITION, END_TRANSITION = Value

  def toString(transition: Value) = {
    transition match {
      case BASIC_TRANSITION ⇒ "BASIC"
      case EXPLORATION_TRANSITION ⇒ "EXPLORATION"
      case AGGREGATION_TRANSITION ⇒ "AGGREGATION"
      case END_TRANSITION ⇒ "END"
      case _ ⇒ throw new UserBadDataError("Unknown transition type " + transition)
    }
  }

  def fromString(transitionString: String) = {
    transitionString match {
      case "BASIC" ⇒ BASIC_TRANSITION
      case "EXPLORATION" ⇒ EXPLORATION_TRANSITION
      case "AGGREGATION" ⇒ AGGREGATION_TRANSITION
      case "END" ⇒ END_TRANSITION
      case _ ⇒ throw new UserBadDataError("Unknown transition type string " + transitionString)
    }
  }
}
