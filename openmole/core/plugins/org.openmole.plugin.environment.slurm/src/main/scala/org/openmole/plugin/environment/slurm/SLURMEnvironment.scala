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

package org.openmole.plugin.environment.slurm

import fr.iscpif.gridscale.ssh.SSHStorage
import fr.iscpif.gridscale.ssh.SSHHost
import java.net.URI
import org.openmole.core.batch.control.LimitedAccess
import org.openmole.core.batch.environment._
import org.openmole.core.batch.storage.PersistentStorageService
import org.openmole.core.batch.storage.StorageService
import org.openmole.misc.workspace._
import org.openmole.plugin.environment.gridscale._
import org.openmole.plugin.environment.ssh._
import fr.iscpif.gridscale.slurm.Gres

object SLURMEnvironment {
  val MaxConnections = new ConfigurationLocation("SLURMEnvironment", "MaxConnections")

  Workspace += (MaxConnections, "10")

  def apply(
    user: String,
    host: String,
    port: Int = 22,
    queue: Option[String] = None,
    openMOLEMemory: Option[Int] = None,
    wallTime: Option[String] = None,
    memory: Option[Int] = None,
    path: Option[String] = None,
    gres: List[Gres] = List(),
    constraints: List[String] = List(),
    workDirectory: Option[String] = None)(implicit authentications: AuthenticationProvider) =
    new SLURMEnvironment(user, host, port, queue, openMOLEMemory, wallTime, memory, path, gres, constraints, workDirectory)
}

import SLURMEnvironment._

class SLURMEnvironment(
    val user: String,
    val host: String,
    override val port: Int,
    val queue: Option[String],
    override val openMOLEMemory: Option[Int],
    val wallTime: Option[String],
    val memory: Option[Int],
    val path: Option[String],
    //override val threads: Option[Int],
    val gres: List[Gres],
    val constraints: List[String],
    //val nodes: Option[Int],
    //val coreByNode: Option[Int],
    val workDirectory: Option[String])(implicit authentications: AuthenticationProvider) extends BatchEnvironment with SSHAccess with MemoryRequirement { env ⇒

  type SS = PersistentStorageService
  type JS = SLURMJobService

  @transient lazy val authentication = SSHAuthentication(user, host, port, authentications)(authentications)
  @transient lazy val id = new URI("slurm", env.user, env.host, env.port, null, null, null).toString

  @transient lazy val storage =
    new PersistentStorageService with SSHStorageService with ThisHost with LimitedAccess {
      def nbTokens = Workspace.preferenceAsInt(MaxConnections)
      val environment = env
      lazy val root = env.path match {
        case Some(p) ⇒ p
        case None    ⇒ child(home, ".openmole")
      }
    }

  @transient lazy val jobService = new SLURMJobService with ThisHost with LimitedAccess {
    def nbTokens = Workspace.preferenceAsInt(MaxConnections)
    def queue = env.queue
    val environment = env
    def sharedFS = storage
    val id = url.toString
  }

  def allStorages = List(storage)
  def allJobServices = List(jobService)

}
