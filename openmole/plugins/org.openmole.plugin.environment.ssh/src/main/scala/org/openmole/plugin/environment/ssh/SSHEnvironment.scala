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

object SSHEnvironment {
  val MaxConnections = new ConfigurationLocation("SSHEnvironment", "MaxConnections")
  
  Workspace += (MaxConnections, "10")
}

import SSHEnvironment._

class SSHEnvironment(login: String, host: String, port: Int, dir: String, override val inMemorySizeForRuntime: Option[Int]) extends BatchEnvironment {
  
  def this(login: String, host: String, port: Int, dir: String) = this(login, host, port, dir, None)

  def allStorages = List(new PersistentStorage(this, URI.create("sftp://" + login + "@" + host + ':' + port + '/' + dir), Workspace.preferenceAsInt(MaxConnections)))
  def allJobServices: Iterable[JobService] = List(new SSHJobService(this, new ServiceDescription("ssh://" + login + '@' + host + ':' + port), Workspace.preferenceAsInt(MaxConnections)))
  
  override def authentication = SSHAuthentication(login, host)
}
