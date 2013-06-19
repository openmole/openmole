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

package org.openmole.plugin.environment.pbs

import fr.iscpif.gridscale.ssh.{ SSHAuthentication, SSHJobService, SSHHost }
import fr.iscpif.gridscale.pbs.{ PBSJobService ⇒ GSPBSJobService, PBSJobDescription }
import java.net.URI
import org.openmole.core.batch.control._
import org.openmole.core.batch.environment._
import org.openmole.core.batch.jobservice.{ BatchJob, BatchJobId }
import org.openmole.plugin.environment.ssh.{ SharedStorage, SSHService }
import org.openmole.core.batch.storage.SimpleStorage
import org.openmole.plugin.environment.gridscale._
import org.openmole.misc.tools.service.Duration._
import org.openmole.misc.workspace.Workspace

trait PBSJobService extends GridScaleJobService with SSHHost with SharedStorage { js ⇒

  def environment: PBSEnvironment

  val jobService = new GSPBSJobService {
    def host = js.host
    def user = js.user
    override def port = js.port
    override def timeout = Workspace.preferenceAsDuration(SSHService.timeout).toMilliSeconds.toInt
  }

  protected def _submit(serializedJob: SerializedJob) = {
    val (remoteScript, result) = buildScript(serializedJob)
    val jobDescription = new PBSJobDescription {
      val executable = "/bin/bash"
      val arguments = remoteScript
      override val queue = environment.queue
      val workDirectory = environment.workDirectory.getOrElse(serializedJob.path)
      override val wallTime = environment.wallTime.map(_.toMinutes)
      override val memory = Some(environment.requieredMemory)
      override val nodes = environment.nodes orElse environment.threads
      override val coreByNode = environment.coreByNode orElse environment.threads
    }

    val jid = js.jobService.submit(jobDescription)(authentication)

    new BatchJob with BatchJobId {
      val jobService = js
      val id = jid
      val resultPath = result
    }
  }

}
