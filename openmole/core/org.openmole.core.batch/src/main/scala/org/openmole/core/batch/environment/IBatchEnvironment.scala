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

package org.openmole.core.batch.environment

import java.io.File
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.model.execution.IEnvironment
import org.openmole.core.model.execution.IExecutionJobRegistry

trait IBatchEnvironment extends IEnvironment {
    def runtime: File
    def allJobServices: Iterable[IBatchJobService[_,_]]
    def allStorages: Iterable[IBatchStorage[_,_]]
//    def jobServices: IBatchServiceGroup[IBatchJobService[_,_]] 
//    def storages: IBatchServiceGroup[IBatchStorage[_,_]]
    def selectAJobService: (IBatchJobService[_,_], AccessToken)
    def selectAStorage: (IBatchStorage[_,_], AccessToken)
    override def jobRegistry: IExecutionJobRegistry[IBatchExecutionJob]
}
