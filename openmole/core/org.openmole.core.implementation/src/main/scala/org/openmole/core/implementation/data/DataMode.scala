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

package org.openmole.core.implementation.data

import org.openmole.core.model.data.{DataModeMask,IDataMode}
import org.openmole.core.model.data.DataModeMask._

object DataMode {
  val NONE = new DataMode(0)
  
  def apply(masks: DataModeMask*) = {
    var mask = 0
    for(m <- masks) mask |= m.value
    new DataMode(mask)
  }
  
}

class DataMode(mask: Int) extends IDataMode {
  override def is(mode: DataModeMask): Boolean = (mask & mode.value) != 0
}
