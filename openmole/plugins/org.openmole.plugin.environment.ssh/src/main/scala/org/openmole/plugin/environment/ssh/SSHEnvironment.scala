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

import org.openmole.core.authentication.AuthenticationStore
import org.openmole.core.preference.{ ConfigurationLocation, Preference }
import org.openmole.core.workflow.dsl._
import org.openmole.core.workspace._
import org.openmole.plugin.environment.batch.control._
import org.openmole.plugin.environment.batch.environment._
import org.openmole.tool.crypto._
import squants.information._
import squants.time.TimeConversions._

object SSHEnvironment {

  val MaxConnections = ConfigurationLocation("SSHEnvironment", "MaxConnections", Some(10))
  val MaxOperationsByMinute = ConfigurationLocation("SSHEnvironment", "MaxOperationByMinute", Some(500))
  val UpdateInterval = ConfigurationLocation("SSHEnvironment", "UpdateInterval", Some(10 seconds))

  def apply(
    user:                 String,
    host:                 String,
    nbSlots:              Int,
    port:                 Int                           = 22,
    sharedDirectory:      OptionalArgument[String]      = None,
    workDirectory:        OptionalArgument[String]      = None,
    openMOLEMemory:       OptionalArgument[Information] = None,
    threads:              OptionalArgument[Int]         = None,
    storageSharedLocally: Boolean                       = false,
    name:                 OptionalArgument[String]      = None
  )(implicit services: BatchEnvironment.Services, cypher: Cypher, authenticationStore: AuthenticationStore, varName: sourcecode.Name) = {
    import services._
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
      name = Some(name.getOrElse(varName.value))
    )(SSHAuthentication.find(user, host, port).apply)
  }
}

class SSHEnvironment(
  val user:                 String,
  val host:                 String,
  val nbSlots:              Int,
  override val port:        Int,
  val sharedDirectory:      Option[String],
  val workDirectory:        Option[String],
  val openMOLEMemory:       Option[Information],
  override val threads:     Option[Int],
  val storageSharedLocally: Boolean,
  override val name:        Option[String]
)(val credential: fr.iscpif.gridscale.ssh.SSHAuthentication)(implicit val services: BatchEnvironment.Services) extends SimpleBatchEnvironment with SSHPersistentStorage { env ⇒

  type JS = SSHJobService

  def id = new URI("ssh", env.user, env.host, env.port, null, null, null).toString

  val usageControl =
    new LimitedAccess(
      preference(SSHEnvironment.MaxConnections),
      preference(SSHEnvironment.MaxOperationsByMinute)
    )

  import services.threadProvider

  val jobService = SSHJobService(
    slots = nbSlots,
    sharedFS = storage,
    environment = env,
    workDirectory = env.workDirectory,
    credential = credential,
    host = host,
    user = user,
    port = port
  )

  override def updateInterval = UpdateInterval.fixed(preference(SSHEnvironment.UpdateInterval))

  override def start() = {
    super.start()
    jobService.start()
  }

  override def stop() = {
    super.stop()
    jobService.stop()
  }

}