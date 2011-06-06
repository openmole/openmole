/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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

package org.openmole.ide.core.commons

 object CapsuleType extends Enumeration {
    type CapsuleType= Value
    val CAPSULE,EXPLORATION_TASK,BASIC_TASK= Value
    
//  def toString(transition: Value) = {
//    transition match {
//      case CAPSULE => "CAPSULE"
//      case EXPLORATION_TASK=> "EXPLORATION"
//      case AGGREGATION_TRANSITION=> "AGGREGATION"
//      case _=> throw new GUIUserBadDataError("Unknown transition type " + transition)
//    }
//  }
//  
//  def fromString(transitionString: String) = {
//    transitionString match {
//      case "BASIC" => BASIC_TRANSITION
//      case "EXPLORATION"=> EXPLORATION_TRANSITION
//      case "AGGREGATION"=> AGGREGATION_TRANSITION
//      case _=> throw new GUIUserBadDataError("Unknown transition type string " + transitionString)
//    }
//  }
  }
