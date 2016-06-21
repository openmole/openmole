/*
 * Copyright (C) 2011 Romain Reuillon
 * Copyright (C) 2014 Jonathan Passerat-Palmbach
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

import org.openmole.core.batch.control.UsageControl
import org.openmole.core.batch.environment._
import org.openmole.core.batch.jobservice.BatchJob
import org.openmole.core.event._
import org.openmole.core.workflow.execution.ExecutionState
import org.openmole.core.workspace.Workspace
import org.openmole.plugin.environment.gridscale._
import fr.iscpif.gridscale.ssh.{ SSHConnectionCache, SSHJobDescription, SSHJobService ⇒ GSSSHJobService }
import java.util.concurrent.atomic.AtomicInteger

import org.openmole.tool.logger.Logger

import scala.collection.mutable

object SSHJobService extends Logger

import SSHJobService._

trait SSHJobService extends GridScaleJobService with SharedStorage { js ⇒

  def environment: SSHEnvironment

  def nbSlots: Int
  override def usageControl = environment.usageControl

  val jobService = new GSSSHJobService with SSHConnectionCache {
    override def timeout = Workspace.preference(SSHService.timeout)
    override def credential = environment.credential
    override def host = environment.host
    override def port = environment.port
  }

  // replaced mutable.SynchronizedQueue according to recommendation in scala doc
  val queue = mutable.Stack[SSHBatchJob]()
  @transient lazy val nbRunning = new AtomicInteger

  protected def _submit(serializedJob: SerializedJob) = {
    val (remoteScript, result) = buildScript(serializedJob)

    val _jobDescription = SSHJobDescription(
      executable = "/bin/bash",
      arguments = remoteScript,
      workDirectory = sharedFS.root
    )

    val sshBatchJob = new SSHBatchJob {
      val jobService = js
      val jobDescription = _jobDescription
      val resultPath = result
    }

    Log.logger.fine(s"SSHJobService: Queueing /bin/bash $remoteScript in directory ${sharedFS.root}")

    import ExecutionState._

    sshBatchJob listen {
      case (_, ev: BatchJob.StateChanged) ⇒
        ev.newState match {
          case DONE | FAILED | KILLED ⇒
            ev.oldState match {
              case DONE | FAILED | KILLED ⇒
              case _ ⇒
                queue.synchronized {
                  if (!queue.isEmpty) Some(queue.pop) else None
                } match {
                  case Some(j) ⇒ j.submit
                  case None ⇒
                    nbRunning.decrementAndGet
                    Log.logger.fine(s"SSHJobService: ${nbRunning.get()} on $nbSlots taken")
                }
            }
          case _ ⇒
        }
    }

    queue.synchronized {
      Log.logger.fine(s"SSHJobService: ${nbRunning.get()} on $nbSlots taken")
      if (nbRunning.get() < nbSlots) {
        nbRunning.incrementAndGet
        sshBatchJob.submit
      }
      else queue.push(sshBatchJob)
    }
    sshBatchJob
  }

  private[ssh] def submit(description: SSHJobDescription) =
    jobService.submit(description)

}
