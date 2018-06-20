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
import org.openmole.plugin.environment.batch.jobservice._
import org.openmole.plugin.environment.batch.storage._
import squants.time.Time

sealed trait JobMessage
sealed trait DispatchedMessage
case class Upload(job: BatchExecutionJob) extends JobMessage with DispatchedMessage
case class Uploaded(job: BatchExecutionJob, serializedJob: SerializedJob) extends JobMessage
case class Submit(job: BatchExecutionJob, serializedJob: SerializedJob) extends JobMessage with DispatchedMessage
case class Submitted(job: BatchExecutionJob, serializedJob: SerializedJob, batchJob: BatchJobControl) extends JobMessage
case class Refresh(job: BatchExecutionJob, serializedJob: SerializedJob, batchJob: BatchJobControl, delay: Time, consecutiveUpdateErrors: Int = 0) extends JobMessage with DispatchedMessage
case class Resubmit(job: BatchExecutionJob, storage: StorageService[_]) extends JobMessage
case class Delay(msg: JobMessage, delay: Time) extends JobMessage
case class Error(job: BatchExecutionJob, exception: Throwable, batchJob: Option[(String, String)]) extends JobMessage with DispatchedMessage
case class Kill(job: BatchExecutionJob) extends JobMessage
case class KillBatchJob(batchJob: BatchJobControl) extends JobMessage with DispatchedMessage
case class GetResult(job: BatchExecutionJob, serializedJob: SerializedJob, outputFilePath: String) extends JobMessage with DispatchedMessage
case class Manage(job: BatchExecutionJob) extends JobMessage
case class MoleJobError(moleJob: MoleJob, job: BatchExecutionJob, exception: Throwable) extends JobMessage
case class CleanSerializedJob(job: SerializedJob) extends JobMessage with DispatchedMessage
case class DeleteFile(storage: StorageService[_], path: String, directory: Boolean) extends JobMessage with DispatchedMessage
