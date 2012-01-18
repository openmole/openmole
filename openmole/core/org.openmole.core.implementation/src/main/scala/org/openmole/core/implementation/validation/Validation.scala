/*
 * Copyright (C) 2012 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

package org.openmole.core.implementation.validation

import org.openmole.core.model.mole.IMole
import TypeUtil.receivedTypes
import org.openmole.core.model.data.DataModeMask._
import scala.collection.immutable.TreeMap
import org.openmole.misc.tools.obj.ClassUtils._
import DataflowProblem._

object Validation {
  
  def typeErrors(mole: IMole) = 
    mole.capsules
      .flatMap {
        c => c.intputSlots.map {
          s => (c, s, TreeMap(receivedTypes(s).map{p => p.name -> p}.toSeq: _*))
        }
      }.flatMap {
        case(capsule, slot, received) =>
          capsule.inputs.filterNot(_.mode is optional).flatMap(
            input => 
              received.get(input.prototype.name) match {
                case Some(recieved) => 
                  if(!input.prototype.isAssignableFrom(recieved)) Some(new WrongType(capsule, slot, input, recieved))
                  else None
                case None => Some(new MissingInput(capsule, slot, input))
              }
          )
      }
      
}
