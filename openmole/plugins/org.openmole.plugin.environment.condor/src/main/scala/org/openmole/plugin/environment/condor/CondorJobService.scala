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

package org.openmole.plugin.environment.condor

import fr.iscpif.gridscale.ssh.{ SSHConnectionCache, SSHAuthentication, SSHJobService, SSHHost }
import fr.iscpif.gridscale.condor.{ CondorJobService ⇒ GSCondorJobService, CondorJobDescription }
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

object CondorJobService extends Logger

import CondorJobService._

trait CondorJobService extends GridScaleJobService with SSHHost with SharedStorage { js ⇒

  def environment: CondorEnvironment

  val jobService = new GSCondorJobService with SSHConnectionCache {
    def host = js.host
    def user = js.user
    def credential = js.credential
    override def port = js.port
    override def timeout = Workspace.preferenceAsDuration(SSHService.timeout)
  }

  protected def _submit(serializedJob: SerializedJob) = {
    val (remoteScript, result) = buildScript(serializedJob)
    val jobDescription = new CondorJobDescription {
      val executable = "/bin/bash"
      val arguments = remoteScript
      // TODO not available in GridScale plugin yet
      //override val queue = environment.queue
      val workDirectory = serializedJob.path
      // TODO not available in GridScale plugin yet
      //override val wallTime = environment.wallTime
      override val memory = Some(environment.requiredMemory)
      override val nodes = environment.nodes
      // TODO typo in coreByNode in GridScale -> should be coresByNode
      override val coreByNode = environment.coresByNode orElse environment.threads
      override val requirements = environment.requirements
    }

    val jid = js.jobService.submit(jobDescription)
    Log.logger.fine(s"Condor job [${jid.condorId}], description: \n ${jobDescription.toCondor}")

    new BatchJob with BatchJobId {
      val jobService = js
      val id = jid
      val resultPath = result
    }
  }

}
