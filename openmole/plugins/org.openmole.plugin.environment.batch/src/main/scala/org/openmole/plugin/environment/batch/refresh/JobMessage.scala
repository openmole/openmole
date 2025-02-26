/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.plugin.environment.batch.refresh

import org.openmole.core.workflow.job._
import org.openmole.plugin.environment.batch.environment._
import squants.time.Time

sealed trait JobMessage
sealed trait DispatchedMessage

case class Submit(job: BatchExecutionJob, environment: BatchEnvironment) extends JobMessage with DispatchedMessage
case class Submitted(job: BatchExecutionJob, environment: BatchEnvironment, batchJob: BatchJobControl) extends JobMessage
case class Refresh(job: BatchExecutionJob, environment: BatchEnvironment, batchJob: BatchJobControl, delay: Time, consecutiveUpdateErrors: Int = 0) extends JobMessage with DispatchedMessage
case class Delay(msg: JobMessage, delay: Time) extends JobMessage
case class Error(job: BatchExecutionJob, environment: BatchEnvironment, exception: Throwable, stdOutErr: Option[(String, String)], output: Option[String]) extends JobMessage with DispatchedMessage
case class Kill(job: BatchExecutionJob, environment: BatchEnvironment, batchJob: Option[BatchJobControl]) extends JobMessage with DispatchedMessage
case class GetResult(job: BatchExecutionJob, environment: BatchEnvironment, outputFilePath: String, batchJob: BatchJobControl) extends JobMessage with DispatchedMessage
case class Manage(job: BatchExecutionJob, environment: BatchEnvironment) extends JobMessage
case class MoleJobError(moleJob: JobId, job: BatchExecutionJob, environment: BatchEnvironment, exception: Throwable, output: Option[String], host: String) extends JobMessage
case class RetryAction(action: () => Boolean) extends JobMessage with DispatchedMessage
