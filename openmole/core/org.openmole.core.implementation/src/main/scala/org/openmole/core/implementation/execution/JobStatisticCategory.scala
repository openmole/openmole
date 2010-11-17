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

package org.openmole.core.implementation.execution

import org.openmole.core.model.execution.IJobStatisticCategory
import org.openmole.core.model.job.IJob

class JobStatisticCategory(val fingerPrint: Array[Object]) extends IJobStatisticCategory {

  def this(job: IJob) = this(Array[Object](job.moleJobs.map( _.task) ++ JobRegistry(job)))

  override def hashCode = fingerPrint.deep.hashCode

  override def equals(obj: Any): Boolean = {
    if(obj == null) return false
    if(!getClass.isAssignableFrom(obj.asInstanceOf[AnyRef].getClass)) return false
    
    fingerPrint.deep == obj.asInstanceOf[JobStatisticCategory].fingerPrint.deep
  }
}
