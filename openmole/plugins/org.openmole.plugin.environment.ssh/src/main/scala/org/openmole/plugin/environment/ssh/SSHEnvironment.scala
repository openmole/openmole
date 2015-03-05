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
import org.openmole.core.batch.storage.PersistentStorageService
import org.openmole.core.workspace.{ Workspace, ConfigurationLocation, AuthenticationProvider }
import concurrent.duration._

object SSHEnvironment {
  val MaxConnections = new ConfigurationLocation("SSHEnvironment", "MaxConnections")
  val ConnectionsKeepAlive = new ConfigurationLocation("SSHEnvironment", "ConnectionsKeepAlive")
  val UpdateInterval = new ConfigurationLocation("SSHEnvironment", "UpdateInterval")

  Workspace += (UpdateInterval, "PT10S")
  Workspace += (ConnectionsKeepAlive, "PT2M")
  Workspace += (MaxConnections, "10")

  def apply(
    user: String,
    host: String,
    nbSlots: Int,
    port: Int = 22,
    workDirectory: Option[String] = None,
    openMOLEMemory: Option[Int] = None,
    threads: Option[Int] = None)(implicit authentications: AuthenticationProvider) =
    new SSHEnvironment(user, host, nbSlots, port, workDirectory, openMOLEMemory, threads)
}

import SSHEnvironment._

class SSHEnvironment(
    val user: String,
    val host: String,
    val nbSlots: Int,
    override val port: Int,
    val workDirectory: Option[String],
    override val openMOLEMemory: Option[Int],
    override val threads: Option[Int])(implicit authentications: AuthenticationProvider) extends BatchEnvironment with SSHPersistentStorage { env ⇒

  type JS = SSHJobService

  @transient lazy val credential = SSHAuthentication(user, host, port, authentications)(authentications)
  def id = new URI("ssh", env.user, env.host, env.port, null, null, null).toString

  @transient lazy val jobService = new SSHJobService with LimitedAccess with ThisHost {
    def nbTokens = maxConnections
    def nbSlots = env.nbSlots
    def sharedFS = storage
    val environment = env
  }

  def allJobServices = List(jobService)

  override def minUpdateInterval = Workspace.preferenceAsDuration(UpdateInterval)
  override def maxUpdateInterval = Workspace.preferenceAsDuration(UpdateInterval)
  override def incrementUpdateInterval = 0 second

}