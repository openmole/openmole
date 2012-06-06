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

import java.util.UUID
import org.ogf.saga.job.Job
import org.ogf.saga.job.JobDescription
import org.ogf.saga.job.JobFactory
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.control.ServiceDescription
import org.openmole.core.batch.control.UsageControl.withToken
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.core.batch.environment.Runtime
import org.openmole.core.batch.environment.BatchJob
import org.openmole.core.batch.environment.JobService
import org.openmole.core.batch.environment.SerializedJob
import org.openmole.core.batch.environment.Storage
import org.openmole.core.batch.file.URIFile
import org.openmole.core.model.execution.ExecutionState
import org.openmole.misc.eventdispatcher.Event
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.eventdispatcher.EventListener
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.workspace.Workspace
import org.openmole.plugin.environment.jsaga.JSAGAJob
import org.openmole.plugin.environment.jsaga.JSAGAJobService
import java.net.URI
import SSHBatchJob._
import org.openmole.plugin.environment.jsaga.SharedFSJobService
import scala.collection.immutable.TreeSet

object SSHJobService extends Logger

import SSHJobService._

class SSHJobService(
    val uri: URI,
    val environment: SSHEnvironment,
    val nbSlot: Int,
    val sharedFS: Storage,
    override val nbAccess: Int) extends JSAGAJobService with SharedFSJobService {

  var queue = new TreeSet[SSHBatchJob]
  var nbRunning = 0

  object BatchJobStatusListner extends EventListener[BatchJob] {

    import ExecutionState._

    override def triggered(job: BatchJob, ev: Event[BatchJob]) = SSHJobService.this.synchronized {
      ev match {
        case ev: BatchJob.StateChanged ⇒
          ev.newState match {
            case DONE | KILLED | FAILED ⇒
              queue -= job.asInstanceOf[SSHBatchJob]
              ev.oldState match {
                case DONE | FAILED | KILLED ⇒
                case _ ⇒
                  val sshJob = queue.headOption match {
                    case Some(j) ⇒
                      queue -= j
                      j.unqueue
                    case None ⇒ nbRunning -= 1
                  }

              }
            case _ ⇒
          }
      }
    }
  }

  protected def doSubmit(serializedJob: SerializedJob, token: AccessToken) = {
    val (remoteScript, result) = buildScript(serializedJob, token)
    val jobDesc = JobFactory.createJobDescription
    jobDesc.setAttribute(JobDescription.EXECUTABLE, "/bin/bash")
    jobDesc.setVectorAttribute(JobDescription.ARGUMENTS, Array[String](remoteScript.path))

    val job = jobService.createJob(jobDesc)
    val sshJob = new SSHBatchJob(job, result.path, this)

    EventDispatcher.listen(sshJob: BatchJob, BatchJobStatusListner, classOf[BatchJob.StateChanged])

    synchronized {
      if (nbRunning < nbSlot) {
        nbRunning += 1
        sshJob.unqueue
      } else queue += sshJob
    }
    sshJob
  }

}
