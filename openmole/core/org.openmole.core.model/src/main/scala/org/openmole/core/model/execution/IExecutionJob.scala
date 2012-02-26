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

package org.openmole.core.model.execution

import org.openmole.core.model.job.IJob
import org.openmole.core.model.tools.ITimeStamp
import ExecutionState._

/*object IExecutionJob {
  case class ExceptionRaised(val exception: Throwable, level: Level) extends Event[IExecutionJob]
  case class StateChanged(val newState: ExecutionState, oldState: ExecutionState) extends Event[IExecutionJob]
}*/

trait IExecutionJob {  
  def state: ExecutionState
  def environment: IEnvironment
  def job: IJob
  def timeStamps: Seq[ITimeStamp[ExecutionState]]
}
