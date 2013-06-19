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
import fr.iscpif.gridscale.ssh.SSHHost
import fr.iscpif.gridscale.ssh.{ SSHJobDescription, SSHJobService ⇒ GSSSHJobService, SSHAuthentication ⇒ GSSSHAuthentication }
import java.net.URI
import scala.collection.immutable.TreeSet
import java.util.concurrent.atomic.AtomicInteger
import collection.mutable

object SSHJobService extends Logger

import SSHJobService._

trait SSHJobService extends GridScaleJobService with SharedStorage with LimitedAccess { js ⇒

  def nbSlots: Int

  val jobService = new GSSSHJobService {
    def host = js.host
    override def port = js.port
    def user = js.user
    override def timeout = Workspace.preferenceAsDuration(SSHService.timeout).toMilliSeconds.toInt
  }

  val queue = new mutable.SynchronizedQueue[SSHBatchJob]
  @transient lazy val nbRunning = new AtomicInteger

  object BatchJobStatusListner extends EventListener[BatchJob] {

    import ExecutionState._

    override def triggered(job: BatchJob, ev: Event[BatchJob]) = SSHJobService.this.synchronized {
      ev match {
        case ev: BatchJob.StateChanged ⇒
          ev.newState match {
            case DONE | KILLED | FAILED ⇒
              ev.oldState match {
                case DONE | FAILED | KILLED ⇒
                case _ ⇒
                  queue.dequeueFirst(_ ⇒ true) match {
                    case Some(j) ⇒ j.submit
                    case None    ⇒ nbRunning.decrementAndGet
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
      if (nbRunning.get() < nbSlots) {
        nbRunning.incrementAndGet
        sshBatchJob.submit
      }
      else queue.enqueue(sshBatchJob)
    }
    sshBatchJob
  }

  private[ssh] def submit(description: SSHJobDescription) =
    jobService.submit(description)(authentication)

}
