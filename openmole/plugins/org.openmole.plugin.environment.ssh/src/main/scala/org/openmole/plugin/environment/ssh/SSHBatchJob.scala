/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.plugin.environment.ssh

import org.ogf.saga.job.Job
import org.openmole.core.batch.environment.BatchJob
import org.openmole.core.model.execution.ExecutionState._
import org.openmole.plugin.environment.jsaga.JSAGAJob
import scala.actors.threadpool.AtomicInteger

object SSHBatchJob {

  val id = new AtomicInteger
  implicit val oder = Ordering.by[SSHBatchJob, Int](j ⇒ j.id)

}

class SSHBatchJob(job: Job, override val resultPath: String, jobService: SSHJobService) extends JSAGAJob(resultPath, jobService) {

  val id = SSHBatchJob.id.getAndIncrement
  var jobIdOption: Option[String] = None

  def jobId = jobIdOption match {
    case Some(jobId) ⇒ jobId
    case None ⇒ throw new InternalError("Bug: JSAGA job is not lanched yet.")
  }

  def unqueue = synchronized {
    job.run
    jobIdOption = Some(JSAGAJob.id(job))
  }

  override def deleteJob = synchronized {
    jobIdOption match {
      case Some(j) ⇒ super.deleteJob
      case None ⇒
    }
  }

  override def updatedState = synchronized {
    jobIdOption match {
      case Some(j) ⇒ super.updatedState
      case None ⇒ SUBMITTED
    }
  }

}
