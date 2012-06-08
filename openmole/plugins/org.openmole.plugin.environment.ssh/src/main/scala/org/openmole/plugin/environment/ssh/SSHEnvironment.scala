/*
 * Copyright (C) 2011 reuillon
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
import org.openmole.core.batch.control.ServiceDescription
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.core.batch.environment.JobService
import org.openmole.core.batch.environment.PersistentStorage
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.workspace.Workspace
import org.openmole.plugin.environment.jsaga.JSAGAEnvironment

object SSHEnvironment {
  val MaxConnections = new ConfigurationLocation("SSHEnvironment", "MaxConnections")
  val UpdateInterval = new ConfigurationLocation("SSHEnvironment", "UpdateInterval")

  Workspace += (UpdateInterval, "PT10S")
  Workspace += (MaxConnections, "5")
}

import SSHEnvironment._

class SSHEnvironment(
    login: String,
    host: String,
    nbSlots: Int,
    dir: String = "/tmp/",
    val runtimeMemory: Int = BatchEnvironment.defaultRuntimeMemory) extends JSAGAEnvironment {

  def requirements = List.empty

  val storage = PersistentStorage.createBaseDir(this, URI.create("sftp://" + login + "@" + host), dir, Workspace.preferenceAsInt(MaxConnections))

  val jobService = new SSHJobService(
    URI.create("ssh://" + login + '@' + host),
    this,
    nbSlots,
    storage,
    Workspace.preferenceAsInt(MaxConnections))

  def allStorages = List(storage)
  def allJobServices: Iterable[JobService] = List(jobService)

  override def authentication = SSHAuthentication(login, host)

  override def minUpdateInterval = Workspace.preferenceAsDurationInMs(UpdateInterval)
  override def maxUpdateInterval = Workspace.preferenceAsDurationInMs(UpdateInterval)
  override def incrementUpdateInterval = 0

}