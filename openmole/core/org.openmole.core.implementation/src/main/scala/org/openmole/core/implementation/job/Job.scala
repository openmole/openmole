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

package org.openmole.core.implementation.job

import org.openmole.core.model.job.IJob
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.job.MoleJobId
import scala.collection.immutable.TreeMap

class Job extends IJob {

    var _moleJobs = new TreeMap[MoleJobId, IMoleJob]

    override def moleJobs: Iterable[IMoleJob] = _moleJobs.values
       
    override def apply(id: MoleJobId) = _moleJobs(id)

    def +=(moleJob: IMoleJob) = synchronized { _moleJobs += ((moleJob.id, moleJob)) }

    override def allMoleJobsFinished: Boolean = {
        for(moleJob <- moleJobs) {
            if(!moleJob.isFinished) {
                return false
            }
         }
         true
    }
}
