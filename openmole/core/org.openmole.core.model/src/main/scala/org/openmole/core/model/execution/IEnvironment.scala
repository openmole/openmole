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


import java.util.logging.Level
import org.openmole.core.model.job.IJob
import org.openmole.misc.eventdispatcher.Event

object IEnvironment {
  case class JobSubmitted(val job: IExecutionJob) extends Event[IEnvironment]
  case class ExceptionRaised(val job: IExecutionJob, val exception: Throwable, level: Level) extends Event[IEnvironment]
}

trait IEnvironment {
  def submit(job: IJob)
}
