/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.batch.environment

import org.openmole.core.model.job.IJob
import scala.collection.mutable.HashMap
import scala.collection.mutable.Set
import scala.collection.mutable.MultiMap

class ExecutionJobRegistry {

  val jobs = new HashMap[IJob, Set[BatchExecutionJob]] with MultiMap[IJob, BatchExecutionJob]

  def allJobs: Iterable[IJob] = synchronized { jobs.keySet }

  def executionJobs(job: IJob): Iterable[BatchExecutionJob] = synchronized { jobs.getOrElse(job, Set.empty) }

  def remove(ejob: BatchExecutionJob) = synchronized { jobs.removeBinding(ejob.job, ejob) }

  def isEmpty: Boolean = synchronized { jobs.isEmpty }

  def register(ejob: BatchExecutionJob) = synchronized { jobs.addBinding(ejob.job, ejob) }

  def removeJob(job: IJob) = synchronized { jobs -= job }

  def allExecutionJobs: Iterable[BatchExecutionJob] = synchronized { jobs.values.flatten }

}
