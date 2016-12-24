/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.plugin.environment.sge

import fr.iscpif.gridscale.sge.{ SGEJobDescription, SGEJobService ⇒ GSSGEJobService }
import fr.iscpif.gridscale.ssh.{ SSHConnectionCache, SSHHost }
import org.openmole.core.workspace.Workspace
import org.openmole.plugin.environment.batch.environment._
import org.openmole.plugin.environment.batch.jobservice.{ BatchJob, BatchJobId }
import org.openmole.plugin.environment.ssh.{ ClusterJobService, SSHService, SharedStorage }
import org.openmole.tool.logger.Logger

object SGEJobService extends Logger

import org.openmole.plugin.environment.sge.SGEJobService._
import squants.time.TimeConversions._

trait SGEJobService extends ClusterJobService with SSHHost with SharedStorage { js ⇒

  def environment: SGEEnvironment

  val jobService = new GSSGEJobService with SSHConnectionCache {
    def host = js.host
    def user = js.user
    def credential = js.credential
    override def port = js.port
    override def timeout = Workspace.preference(SSHService.timeout)
  }

  protected def _submit(serializedJob: SerializedJob) = {
    val (remoteScript, result) = buildScript(serializedJob)
    val jobDescription = SGEJobDescription(
      executable = "/bin/bash",
      arguments = remoteScript,
      queue = environment.queue,
      workDirectory = serializedJob.path,
      wallTime = environment.wallTime.map(x ⇒ x: concurrent.duration.Duration),
      memory = Some(environment.requiredMemory.toMegabytes.toInt)
    )

    val jid = js.jobService.submit(jobDescription)
    Log.logger.fine(s"SGE job [${jid.sgeId}], description: \n ${jobDescription.toSGE}")

    new BatchJob with BatchJobId {
      val jobService = js
      val id = jid
      val resultPath = result
    }
  }

}
