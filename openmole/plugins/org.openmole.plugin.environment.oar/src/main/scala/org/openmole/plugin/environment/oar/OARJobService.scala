///*
// * Copyright (C) 2012 Romain Reuillon
// * Copyright (C) 2014 Jonathan Passerat-Palmbach
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//
//package org.openmole.plugin.environment.oar
//
//import fr.iscpif.gridscale.oar.{ OARJobDescription, OARJobService ⇒ GSOARJobService }
//import fr.iscpif.gridscale.ssh.SSHConnectionCache
//import org.openmole.core.workspace.Workspace
//import org.openmole.plugin.environment.batch.environment._
//import org.openmole.plugin.environment.batch.jobservice.{ BatchJob, BatchJobId }
//import org.openmole.plugin.environment.ssh.{ ClusterJobService, SSHService }
//import org.openmole.tool.logger.Logger
//import squants.time.TimeConversions._
//
//object OARJobService extends Logger
//
//import org.openmole.plugin.environment.oar.OARJobService._
//
//trait OARJobService extends ClusterJobService { js ⇒
//
//  val environment: OAREnvironment
//
//  val jobService = new GSOARJobService with SSHConnectionCache {
//    def host = js.host
//    def user = js.user
//    def credential = js.credential
//    def usageControl = environment.usageControl
//    override def port = js.port
//    override def timeout = environment.preference(SSHService.timeout)
//  }
//
//  protected def _submit(serializedJob: SerializedJob) = {
//    val (remoteScript, result) = buildScript(serializedJob)
//    val jobDescription = OARJobDescription(
//      executable = "/bin/bash",
//      arguments = remoteScript,
//      queue = environment.queue,
//      workDirectory = serializedJob.path,
//      cpu = environment.cpu,
//      core = environment.core,
//      wallTime = environment.wallTime.map(x ⇒ x: concurrent.duration.Duration),
//      bestEffort = environment.bestEffort
//    )
//
//    val jid = js.jobService.submit(jobDescription)
//    Log.logger.fine(s"OAR job [${jid.id}], description: \n ${jobDescription}")
//
//    new BatchJob with BatchJobId {
//      val jobService = js
//      val id = jid
//      val resultPath = result
//    }
//  }
//
//}
