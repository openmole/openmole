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

package org.openmole.core.implementation.execution

import org.openmole.core.model.execution.IStatisticKey
import org.openmole.core.model.job.IJob
import org.openmole.core.model.task.ITask

import scala.collection.JavaConversions._


class StatisticKey(val key: Array[ITask]) extends IStatisticKey {

  def this(job: IJob) = this(job.moleJobs.toList.map( _.task ).distinct.toArray)

  override def hashCode: Int = key.deep.hashCode

  override def equals(obj: Any): Boolean = {
    if (obj == null) {
      return false
    } 
      
    if (getClass != obj.asInstanceOf[AnyRef].getClass) {
      return false
    }
    val other = obj.asInstanceOf[StatisticKey]
    if (this.key.deep != other.key.deep) {
      return false
    }
    return true
  }
  
}
