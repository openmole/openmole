/*
 * Copyright (C) 2012 Romain Reuillon
 * Copyright (C) 2014 Jonathan Passerat-Palmbach

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

import fr.iscpif.gridscale.ssh.{ SSHConnectionCache, SSHAuthentication, SSHJobService, SSHHost }
import fr.iscpif.gridscale.pbs.{ PBSJobService ⇒ GSPBSJobService, PBSJobDescription }
import java.net.URI
import org.openmole.core.batch.control._
import org.openmole.core.batch.environment._
import org.openmole.core.batch.jobservice.{ BatchJob, BatchJobId }
import org.openmole.core.tools.service.Logger
import org.openmole.core.workspace.Workspace
import org.openmole.plugin.environment.ssh.{ SharedStorage, SSHService }
import org.openmole.core.batch.storage.SimpleStorage
import org.openmole.plugin.environment.gridscale._
import concurrent.duration._

object PBSJobService extends Logger

import PBSJobService._

trait PBSJobService extends GridScaleJobService with SSHHost with SharedStorage { js ⇒

  def environment: PBSEnvironment

  val jobService = new GSPBSJobService with SSHConnectionCache {
    def host = js.host
    def user = js.user
    def credential = js.credential
    override def port = js.port
    override def timeout = Workspace.preferenceAsDuration(SSHService.timeout)
  }

  protected def _submit(serializedJob: SerializedJob) = {
    val (remoteScript, result) = buildScript(serializedJob)
    val jobDescription = new PBSJobDescription {
      val executable = "/bin/bash"
      val arguments = remoteScript
      override val queue = environment.queue
      val workDirectory = serializedJob.path
      override val wallTime = environment.wallTime
      override val memory = Some(environment.requiredMemory)
      override val nodes = environment.nodes
      override val coreByNode = environment.coreByNode orElse environment.threads
    }

    val jid = js.jobService.submit(jobDescription)
    Log.logger.fine(s"PBS job [${jid.pbsId}], description: \n ${jobDescription}")

    new BatchJob with BatchJobId {
      val jobService = js
      val id = jid
      val resultPath = result
    }
  }

}
