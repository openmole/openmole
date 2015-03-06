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

import fr.iscpif.gridscale.ssh.SSHStorage
import fr.iscpif.gridscale.ssh.SSHHost
import fr.iscpif.gridscale.condor.CondorRequirement
import java.net.URI
import org.openmole.core.batch.control.LimitedAccess
import org.openmole.core.batch.environment._
import org.openmole.core.batch.storage.PersistentStorageService
import org.openmole.core.batch.storage.StorageService
import org.openmole.core.workspace.AuthenticationProvider
import org.openmole.plugin.environment.gridscale._
import org.openmole.plugin.environment.ssh._

import scala.concurrent.duration.Duration

object CondorEnvironment {
  def apply(
    user: String,
    host: String,
    port: Int = 22,
    // TODO not available in the GridScale plugin yet
    //  queue: Option[String] = None,
    openMOLEMemory: Option[Int] = None,
    // TODO not available in the GridScale plugin yet
    //wallTime: Option[Duration] = None,
    memory: Option[Int] = None,
    nodes: Option[Int] = None,
    coresByNode: Option[Int] = None,
    workDirectory: Option[String] = None,
    requirements: List[CondorRequirement] = List(),
    threads: Option[Int] = None)(implicit authentications: AuthenticationProvider) =
    new CondorEnvironment(user, host, port, openMOLEMemory, memory, nodes, coresByNode, workDirectory, requirements, threads)
}

class CondorEnvironment(
    val user: String,
    val host: String,
    override val port: Int,
    // TODO not available in the GridScale plugin yet
    //val queue: Option[String],
    override val openMOLEMemory: Option[Int],
    // TODO not available in the GridScale plugin yet
    //val wallTime: Option[Duration],
    val memory: Option[Int],
    val nodes: Option[Int] = None,
    val coresByNode: Option[Int] = None,
    val workDirectory: Option[String],
    val requirements: List[CondorRequirement],
    override val threads: Option[Int])(implicit authentications: AuthenticationProvider) extends BatchEnvironment with SSHPersistentStorage with MemoryRequirement { env ⇒

  type JS = CondorJobService

  @transient lazy val credential = SSHAuthentication(user, host, port, authentications)(authentications)

  @transient lazy val jobService = new CondorJobService with ThisHost with LimitedAccess {
    def nbTokens = maxConnections
    // TODO not available in the GridScale plugin yet
    //def queue = env.queue
    val environment = env
    def sharedFS = storage
  }

  def allJobServices = List(jobService)

}
