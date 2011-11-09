/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.core.implementation.mole

import java.util.Collections
import java.util.TreeMap
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.job.MoleJobId
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.mole.ISubMoleExecution
import org.openmole.core.model.mole.ITicket


object MoleJobRegistry {
  private val jobs = Collections.synchronizedMap(new TreeMap[MoleJobId, (ISubMoleExecution, ICapsule, ITicket)](MoleJobId.moleJobIdOrdering))

  def += (moleJob: IMoleJob, moleExecution: ISubMoleExecution, capsule: ICapsule, ticket: ITicket) {
    jobs.put(moleJob.id, (moleExecution, capsule, ticket))
  }
  
  def remove(moleJob: IMoleJob): Option[(ISubMoleExecution, ICapsule, ITicket)] = jobs.remove(moleJob.id) match {
    case null => None
    case e => Some(e)
  }
  
}
