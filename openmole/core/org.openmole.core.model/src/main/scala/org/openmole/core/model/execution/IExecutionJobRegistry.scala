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

package org.openmole.core.model.execution

import org.openmole.core.model.job.IJob

trait IExecutionJobRegistry  [EXECUTIONJOB <: IExecutionJob] {

    def executionJobs(key: IStatisticKey): Iterable[EXECUTIONJOB]
    
    def jobs(category: IStatisticKey): Iterable[IJob]

    def allExecutionJobs: Iterable[EXECUTIONJOB]

    def allJobs: Iterable[IJob]

    def executionJobs(job: IJob): Iterable[EXECUTIONJOB]

    def lastExecutionJob(job: IJob): Option[EXECUTIONJOB]

    def nbExecutionJobs(job: IJob): Int

    def isEmpty: Boolean

    def register(executionJob: EXECUTIONJOB)
    
    def remove(job: EXECUTIONJOB)

    def removeJob(job: IJob)
}
