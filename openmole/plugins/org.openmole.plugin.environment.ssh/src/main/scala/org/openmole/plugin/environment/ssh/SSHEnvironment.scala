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

import java.net.URI
import org.openmole.core.batch.control._
import org.openmole.core.batch.environment._
import org.openmole.core.workspace._
import concurrent.duration._

object SSHEnvironment {

  val MaxConnections = ConfigurationLocation("SSHEnvironment", "MaxConnections", Some(10))
  val MaxOperationsByMinute = ConfigurationLocation("SSHEnvironment", "MaxOperationByMinute", Some(500))
  val UpdateInterval = ConfigurationLocation("SSHEnvironment", "UpdateInterval", Some(10 seconds))

  Workspace setDefault UpdateInterval
  Workspace setDefault MaxConnections
  Workspace setDefault MaxOperationsByMinute

  def apply(
    user: String,
    host: String,
    nbSlots: Int,
    port: Int = 22,
    sharedDirectory: Option[String] = None,
    workDirectory: Option[String] = None,
    openMOLEMemory: Option[Int] = None,
    threads: Option[Int] = None,
    storageSharedLocally: Boolean = false,
    name: Option[String] = None)(implicit decrypt: Decrypt) =
    new SSHEnvironment(
      user = user,
      host = host,
      nbSlots = nbSlots,
      port = port,
      sharedDirectory = sharedDirectory,
      workDirectory = workDirectory,
      openMOLEMemory = openMOLEMemory,
      threads = threads,
      storageSharedLocally = storageSharedLocally,
      name = name
    )(SSHAuthentication(user, host, port).apply)
}

import SSHEnvironment._

class SSHEnvironment(
    val user: String,
    val host: String,
    val nbSlots: Int,
    override val port: Int,
    val sharedDirectory: Option[String],
    val workDirectory: Option[String],
    override val openMOLEMemory: Option[Int],
    override val threads: Option[Int],
    val storageSharedLocally: Boolean,
    override val name: Option[String])(val credential: fr.iscpif.gridscale.ssh.SSHAuthentication) extends SimpleBatchEnvironment with SSHPersistentStorage { env ⇒

  type JS = SSHJobService

  def id = new URI("ssh", env.user, env.host, env.port, null, null, null).toString

  val usageControl =
    new LimitedAccess(
      Workspace.preference(SSHEnvironment.MaxConnections),
      Workspace.preference(SSHEnvironment.MaxOperationsByMinute)
    )

  @transient lazy val jobService = new SSHJobService with ThisHost {
    def nbSlots = env.nbSlots
    def sharedFS = storage
    val environment = env
    def workDirectory = env.workDirectory
  }

  override def minUpdateInterval = Workspace.preference(UpdateInterval)
  override def maxUpdateInterval = Workspace.preference(UpdateInterval)
  override def incrementUpdateInterval = 0 second

}