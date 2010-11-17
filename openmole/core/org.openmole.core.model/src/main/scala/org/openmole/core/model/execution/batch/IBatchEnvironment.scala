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

package org.openmole.core.model.execution.batch

import java.io.File
import org.openmole.core.model.execution.IEnvironment
import org.openmole.core.model.execution.IExecutionJobRegistry

trait IBatchEnvironment extends IEnvironment {
    def runtime: File
    def allJobServices: Iterable[IBatchJobService[_,_]]
    def allStorages: Iterable[IBatchStorage[_,_]]
    def getJobServices: IBatchServiceGroup[IBatchJobService[_,_]] 
    def getStorages: IBatchServiceGroup[IBatchStorage[_,_]]
    def getAJobService: (IBatchJobService[_,_], IAccessToken)
    def getAStorage: (IBatchStorage[_,_], IAccessToken)
    override def jobRegistry: IExecutionJobRegistry[IBatchExecutionJob]
}
