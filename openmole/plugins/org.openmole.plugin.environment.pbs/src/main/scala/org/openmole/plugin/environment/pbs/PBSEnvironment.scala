/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.plugin.environment.pbs

import java.net.URI
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.core.batch.environment.JobService
import org.openmole.core.batch.environment.PersistentStorage
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.workspace.Workspace
import org.openmole.plugin.environment.jsaga.JSAGAEnvironment
import org.openmole.plugin.environment.jsaga.Requirement

object PBSEnvironment {
  val MaxConnections = new ConfigurationLocation("PBSEnvironment", "MaxConnections")

  Workspace += (MaxConnections, "10")
}

import PBSEnvironment._

class PBSEnvironment(
    login: String,
    host: String,
    dir: String,
    val queue: Option[String] = None,
    val requirements: Iterable[Requirement] = List.empty,
    val runtimeMemory: Int = BatchEnvironment.defaultRuntimeMemory) extends JSAGAEnvironment {

  val storage =
    PersistentStorage.createBaseDir(
      this,
      URI.create("sftp://" + login + "@" + host),
      dir,
      Workspace.preferenceAsInt(MaxConnections))

  val jobService = new PBSJobService(
    URI.create("pbs-ssh://" + login + '@' + host + "/" + dir),
    storage,
    this,
    Workspace.preferenceAsInt(MaxConnections))

  def allStorages = List(storage)
  def allJobServices: Iterable[JobService] = List(jobService)

  override def authentication = PBSAuthentication(login, host)
}
