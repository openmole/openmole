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
import org.openmole.core.batch.environment._
import org.openmole.plugin.environment.gridscale._
import org.openmole.core.batch.storage.PersistentStorageService
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.workspace.Workspace

object SSHEnvironment {
  val MaxConnections = new ConfigurationLocation("SSHEnvironment", "MaxConnections")
  val UpdateInterval = new ConfigurationLocation("SSHEnvironment", "UpdateInterval")

  Workspace += (UpdateInterval, "PT10S")
  Workspace += (MaxConnections, "10")
}

import SSHEnvironment._

class SSHEnvironment(
    val user: String,
    val host: String,
    val nbSlots: Int,
    override val port: Int = 22,
    val path: String = "/tmp/",
    override val runtimeMemory: Option[Int] = None) extends BatchEnvironment with SSHAccess { env ⇒

  type SS = SSHStorageService
  type JS = SSHJobService

  val id = new URI("ssh", env.user, env.host, env.port, null, null, null).toString

  val storage = new PersistentStorageService with SSHStorageService with ThisHost {
    def connections = Workspace.preferenceAsInt(MaxConnections)
    def root = env.path
    def environment = env
  }

  val jobService = new SSHJobService with ThisHost {
    def connections = Workspace.preferenceAsInt(MaxConnections)
    def nbSlots = env.nbSlots
    def root = env.path
    def environment = env
    val id = url.toString
  }

  def allStorages = List(storage)
  def allJobServices = List(jobService)

  override def minUpdateInterval = Workspace.preferenceAsDurationInMs(UpdateInterval)
  override def maxUpdateInterval = Workspace.preferenceAsDurationInMs(UpdateInterval)
  override def incrementUpdateInterval = 0

}