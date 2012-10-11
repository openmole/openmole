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

import java.util.UUID
import org.openmole.core.batch.control._
import org.openmole.core.batch.environment._
import org.openmole.core.batch.jobservice.BatchJob
import org.openmole.core.model.execution.ExecutionState
import org.openmole.misc.eventdispatcher.Event
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.eventdispatcher.EventListener
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.workspace.Workspace
import org.openmole.plugin.environment.gridscale._
import fr.iscpif.gridscale.tools.SSHHost
import fr.iscpif.gridscale.authentication._
import fr.iscpif.gridscale.jobservice.{ SSHJobDescription, SSHJobService ⇒ GSSSHJobService }
import java.net.URI
import SSHBatchJob._
import scala.collection.immutable.TreeSet

object SSHJobService extends Logger

import SSHJobService._

trait SSHJobService extends GridScaleJobService with SharedStorage { js ⇒

  def nbSlots: Int

  val jobService = new GSSSHJobService {
    def host = js.host
    override def port = js.port
    def user = js.user
  }

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
                      j.submit
                    case None ⇒ nbRunning -= 1
                  }

              }
            case _ ⇒
          }
      }
    }
  }

  protected def _submit(serializedJob: SerializedJob) = {
    val (remoteScript, result) = buildScript(serializedJob)

    val _jobDescription = new SSHJobDescription {
      val executable = "/bin/bash"
      val arguments = remoteScript
      val workDirectory = root
    }

    val sshBatchJob = new SSHBatchJob {
      val jobService = js
      val jobDescription = _jobDescription
      val resultPath = result
    }

    EventDispatcher.listen(sshBatchJob: BatchJob, BatchJobStatusListner, classOf[BatchJob.StateChanged])

    synchronized {
      if (nbRunning < nbSlots) {
        nbRunning += 1
        sshBatchJob.submit
      } else queue += sshBatchJob
    }
    sshBatchJob
  }

  private[ssh] def submit(description: SSHJobDescription) =
    jobService.submit(description)(authentication)

}
