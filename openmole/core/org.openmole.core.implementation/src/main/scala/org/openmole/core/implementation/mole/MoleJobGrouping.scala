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

package org.openmole.core.implementation.mole

import org.openmole.core.model.mole.IMoleJobGrouping
import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.core.model.mole.IGroupingStrategy
import scala.collection.mutable.HashMap

object MoleJobGrouping {
  val Empty = new MoleJobGrouping(new HashMap[IGenericCapsule, IGroupingStrategy])
}

class MoleJobGrouping(groupers: HashMap[IGenericCapsule, IGroupingStrategy]) extends IMoleJobGrouping {

    def this() = this(new HashMap[IGenericCapsule, IGroupingStrategy])
    
    override def apply(capsule: IGenericCapsule): Option[IGroupingStrategy] = groupers.get(capsule);
   
    def set(capsule: IGenericCapsule, strategy: IGroupingStrategy) = groupers.put(capsule, strategy);
   
}
