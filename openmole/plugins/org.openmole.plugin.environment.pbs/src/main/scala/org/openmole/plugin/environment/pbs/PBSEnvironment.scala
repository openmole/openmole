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

import fr.iscpif.gridscale.storage.SSHStorage
import fr.iscpif.gridscale.tools.SSHHost
import java.net.URI
import org.openmole.core.batch.authentication.SSHAuthentication
import org.openmole.core.batch.environment._
import org.openmole.core.batch.storage.PersistentStorageService
import org.openmole.core.batch.storage.StorageService
import org.openmole.misc.workspace._
import org.openmole.plugin.environment.gridscale._

object PBSEnvironment {
  val MaxConnections = new ConfigurationLocation("PBSEnvironment", "MaxConnections")

  Workspace += (MaxConnections, "10")
}

import PBSEnvironment._

class PBSEnvironment(
    val user: String,
    val host: String,
    val path: String,
    override val port: Int = 22,
    val queue: Option[String] = None,
    override val runtimeMemory: Option[Int] = None,
    val cpuTime: Option[Int] = None,
    val memory: Option[Int] = None) extends BatchEnvironment with SSHAccess with MemoryRequirement { env ⇒

  type SS = PersistentStorageService
  type JS = PBSJobService

  val id = new URI("pbs", env.user, env.host, env.port, null, null, null).toString

  val storage =
    new PersistentStorageService with SSHStorageService with ThisHost {
      def connections = Workspace.preferenceAsInt(MaxConnections)
      def environment = env
      def root = env.path
    }

  val jobService = new PBSJobService with ThisHost {
    def connections = Workspace.preferenceAsInt(MaxConnections)
    def queue = env.queue
    def environment = env
    def root = env.path
    val id = url.toString
  }

  def allStorages = List(storage)
  def allJobServices = List(jobService)

}
