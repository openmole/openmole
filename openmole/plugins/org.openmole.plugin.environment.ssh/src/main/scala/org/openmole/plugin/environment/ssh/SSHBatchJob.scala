/*
 * Copyright (C) 2011 reuillon
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
  implicit val oder = Ordering.by[SSHBatchJob, Int](j => j.id)
  
}


class SSHBatchJob(job: Job, override val resultPath: String, jobService: SSHJobService) extends BatchJob(jobService) {

  val id = SSHBatchJob.id.getAndIncrement
  var jsagaJob: Option[JSAGAJob] = None
  
  def unqueue = synchronized {
    job.run
    jsagaJob = Some(new JSAGAJob(JSAGAJob.id(job), resultPath, jobService))
  }
  
  def deleteJob = synchronized { 
    jsagaJob match {
      case Some(j) => j.deleteJob 
      case None => 
    }
  }
   
  protected def updatedState = synchronized {    
    jsagaJob match {
      case Some(j) => j.updatedState 
      case None => SUBMITTED
    }
  }

}
