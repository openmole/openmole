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

package org.openmole.core.model.job

import org.openmole.misc.tools.obj.Id

object MoleJobId {
  
  implicit val ordering = new Ordering[MoleJobId] {
    override def compare(left: MoleJobId, right: MoleJobId) = {
      val comp = left.jobId.compare(right.jobId)
      if(comp != 0) comp 
      else left.executionId.compare(right.executionId)
    }
  }
  
}

class MoleJobId(val executionId: String, val jobId: Long) extends Id {
  override def id = (executionId, id)
}

