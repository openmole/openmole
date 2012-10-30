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

package org.openmole.core.batch.refresh

import org.openmole.core.batch.environment._
import org.openmole.core.batch.jobservice._
import org.openmole.core.model.job._
import org.openmole.core.batch.storage._

sealed trait JobMessage
case class Upload(job: BatchExecutionJob) extends JobMessage
case class Uploaded(job: BatchExecutionJob, serializedJob: SerializedJob) extends JobMessage
case class Submit(job: BatchExecutionJob, serializedJob: SerializedJob) extends JobMessage
case class Submitted(job: BatchExecutionJob, serializedJob: SerializedJob, batchJob: BatchJob) extends JobMessage
case class Refresh(job: BatchExecutionJob, serializedJob: SerializedJob, batchJob: BatchJob, delay: Long) extends JobMessage
case class RefreshDelay(job: BatchExecutionJob, serializedJob: SerializedJob, batchJob: BatchJob, delay: Long, statusChanged: Boolean) extends JobMessage
case class Error(job: BatchExecutionJob, exception: Throwable) extends JobMessage
case class Kill(job: BatchExecutionJob) extends JobMessage
case class KillBatchJob(batchJob: BatchJob) extends JobMessage
case class GetResult(job: BatchExecutionJob, serializedJob: SerializedJob, outputFilePath: String) extends JobMessage
case class MoleJobError(moleJob: IMoleJob, job: BatchExecutionJob, exception: Throwable)
case class CleanSerializedJob(job: SerializedJob)
case class DeleteFile(storage: StorageService, path: String, directory: Boolean)
