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

package org.openmole.plugin.environment.oar

import org.openmole.core.authentication.AuthenticationStore
import org.openmole.core.workflow.dsl._
import org.openmole.plugin.environment.batch.environment.BatchEnvironment
import org.openmole.plugin.environment.ssh._
import org.openmole.tool.crypto.Cypher
import squants._
import squants.information._

object OAREnvironment {

  def apply(
    user:                 String,
    host:                 String,
    port:                 Int                           = 22,
    queue:                OptionalArgument[String]      = None,
    core:                 OptionalArgument[Int]         = None,
    cpu:                  OptionalArgument[Int]         = None,
    wallTime:             OptionalArgument[Time]        = None,
    openMOLEMemory:       OptionalArgument[Information] = None,
    sharedDirectory:      OptionalArgument[String]      = None,
    workDirectory:        OptionalArgument[String]      = None,
    threads:              OptionalArgument[Int]         = None,
    storageSharedLocally: Boolean                       = false,
    name:                 OptionalArgument[String]      = None,
    bestEffort:           Boolean                       = true
  )(implicit services: BatchEnvironment.Services, authenticationStore: AuthenticationStore, cypher: Cypher, varName: sourcecode.Name) = {
    import services._
    new OAREnvironment(
      user = user,
      host = host,
      port = port,
      queue = queue,
      core = core,
      cpu = cpu,
      wallTime = wallTime,
      openMOLEMemory = openMOLEMemory,
      sharedDirectory = sharedDirectory,
      workDirectory = workDirectory,
      threads = threads,
      storageSharedLocally = storageSharedLocally,
      name = Some(name.getOrElse(varName.value)),
      bestEffort = bestEffort
    )(SSHAuthentication.find(user, host, port).apply)
  }
}

class OAREnvironment(
  val user:                    String,
  val host:                    String,
  override val port:           Int,
  val queue:                   Option[String],
  val core:                    Option[Int],
  val cpu:                     Option[Int],
  val wallTime:                Option[Time],
  override val openMOLEMemory: Option[Information],
  val sharedDirectory:         Option[String],
  val workDirectory:           Option[String],
  override val threads:        Option[Int],
  val storageSharedLocally:    Boolean,
  override val name:           Option[String],
  val bestEffort:              Boolean
)(val credential: fr.iscpif.gridscale.ssh.SSHAuthentication)(implicit val services: BatchEnvironment.Services) extends ClusterEnvironment { env ⇒

  type JS = OARJobService

  lazy val jobService =
    new OARJobService {
      def queue = env.queue
      val environment = env
      def sharedFS = storage
      def workDirectory = env.workDirectory
      def timeout = env.timeout
      def credential = env.credential
      def user = env.user
      def host = env.host
      def port = env.port
    }

}
