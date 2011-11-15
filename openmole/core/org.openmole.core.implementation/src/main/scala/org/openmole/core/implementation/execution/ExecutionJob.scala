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

import org.openmole.core.model.execution.IEnvironment
import org.openmole.core.model.execution.IExecutionJob
import org.openmole.core.model.execution.IExecutionJobId
import org.openmole.core.model.job.IJob
import org.openmole.core.model.tools.ITimeStamp
import org.openmole.core.implementation.tools.TimeStamp
import scala.collection.mutable.ListBuffer
import  org.openmole.core.model.execution.ExecutionState._

abstract class ExecutionJob[ENV <: IEnvironment](val environment: ENV, val job: IJob, val id: IExecutionJobId) extends IExecutionJob {
   val timeStamps: ListBuffer[ITimeStamp[ExecutionState]] = new ListBuffer
}