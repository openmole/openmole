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

import fr.iscpif.gridscale.condor.CondorRequirement
import fr.iscpif.gridscale.ssh.SSHStorage
import fr.iscpif.gridscale.ssh.SSHHost
import java.net.URI
import org.openmole.core.batch.control.LimitedAccess
import org.openmole.core.batch.environment._
import org.openmole.core.workspace._
import org.openmole.core.workflow.dsl._
import org.openmole.plugin.environment.gridscale._
import org.openmole.plugin.environment.ssh._

import scala.concurrent.duration.Duration

object CondorEnvironment {
  def apply(
    user: String,
    host: String,
    port: Int    = 22,
    // TODO not available in the GridScale plugin yet
    //  queue: Option[String] = None,
    openMOLEMemory: OptionalArgument[Int] = None,
    // TODO not available in the GridScale plugin yet
    //wallTime: Option[Duration] = None,
    memory:               OptionalArgument[Int]               = None,
    nodes:                OptionalArgument[Int]               = None,
    coresByNode:          OptionalArgument[Int]               = None,
    sharedDirectory:      OptionalArgument[String]            = None,
    workDirectory:        OptionalArgument[String]            = None,
    requirements:         OptionalArgument[CondorRequirement] = None,
    threads:              OptionalArgument[Int]               = None,
    storageSharedLocally: Boolean                             = false,
    name:                 OptionalArgument[String]            = None
  )(implicit decrypt: Decrypt) =
    new CondorEnvironment(
      user = user,
      host = host,
      port = port,
      openMOLEMemory = openMOLEMemory,
      memory = memory,
      nodes = nodes,
      coresByNode = coresByNode,
      sharedDirectory = sharedDirectory,
      workDirectory = workDirectory,
      requirements = requirements,
      threads = threads,
      storageSharedLocally = storageSharedLocally,
      name = name
    )(SSHAuthentication.find(user, host, port).apply)
}

class CondorEnvironment(
    val user:          String,
    val host:          String,
    override val port: Int,
    // TODO not available in the GridScale plugin yet
    //val queue: Option[String],
    override val openMOLEMemory: Option[Int],
    // TODO not available in the GridScale plugin yet
    //val wallTime: Option[Duration],
    val memory:               Option[Int],
    val nodes:                Option[Int]               = None,
    val coresByNode:          Option[Int]               = None,
    val sharedDirectory:      Option[String],
    val workDirectory:        Option[String],
    val requirements:         Option[CondorRequirement],
    override val threads:     Option[Int],
    val storageSharedLocally: Boolean,
    override val name:        Option[String]
)(val credential: fr.iscpif.gridscale.ssh.SSHAuthentication) extends ClusterEnvironment with MemoryRequirement { env ⇒

  type JS = CondorJobService

  @transient lazy val jobService = new CondorJobService with ThisHost {
    // TODO not available in the GridScale plugin yet
    //def queue = env.queue
    val environment = env
    def sharedFS = storage
    def workDirectory = env.workDirectory
  }

  def allJobServices = List(jobService)

}
